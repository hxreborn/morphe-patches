/*
 * Copyright (C) 2026 hxreborn
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
#define _GNU_SOURCE

#include <android/log.h>
#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <pthread.h>
#include <stdint.h>
#include <string.h>
#include <sys/mman.h>
#include <time.h>
#include <unistd.h>

#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "hxreborn/ijiami", __VA_ARGS__)
#define LOG_ERROR(...) __android_log_print(ANDROID_LOG_ERROR, "hxreborn/ijiami", __VA_ARGS__)

#define SEARCH_TIMEOUT_MS 4000
#define SEARCH_INTERVAL_US 200
#define REPATCH_TIMEOUT_MS 5000
#define REPATCH_INTERVAL_US 20000

#define REGION_MIN_SIZE 0x10000
#define REGION_MAX_SIZE 0x400000

#define MAPS_BUFFER_SIZE 8192

#define DEX_MAGIC "dex\n"
#define DEX_HEADER_BYTES 112
#define DEX_FILE_SIZE_OFFSET 32
#define DEX_MAP_OFF_OFFSET 52
#define DEX_COPY_LIMIT 8
#define DEX_DATA_REGION "[anon:dalvik-DEX data]"
#define CODE_RECORD_HEADER_BYTES 16

#if defined(__aarch64__)

#define MOV_W0_0 0x52800000
#define MOV_W0_1 0x52800020
#define NOP 0xd503201f

typedef uint32_t code_unit;

#define LOAD_SIGKILL 0x52800121
#define LOAD_SIGNAL_0 0x52800001
#define KILL_SYSCALL_UNITS 2
#define KILL_LOOKAHEAD 6

static const code_unit KILL_SYSCALL[KILL_SYSCALL_UNITS] = {0xd2801028, 0xd4000001};

#elif defined(__arm__)

#define THUMB_MOVS_R0_0_NOP 0xbf002000
#define THUMB_MOVS_R0_1_NOP 0xbf002001
#define THUMB_RETURN_0 0x47702000

typedef uint16_t code_unit;

#define LOAD_SIGKILL 0x2109
#define LOAD_SIGNAL_0 0x2100
#define KILL_SYSCALL_UNITS 3
#define KILL_LOOKAHEAD 8

static const code_unit KILL_SYSCALL[KILL_SYSCALL_UNITS] = {0xf04f, 0x0725, 0xdf00};

#else
#error "unsupported architecture"
#endif

#define KILL_SITE_LIMIT 96
#define SCAN_CHUNK 512
#define SCAN_WINDOW (SCAN_CHUNK + KILL_LOOKAHEAD + KILL_SYSCALL_UNITS - 1)

#include HX_PROFILE

static const struct {
    uint32_t offset;
    uint32_t expected;
    uint32_t replacement;
    const char *checkName;
} PATCHES[] = HX_PROFILE_PATCHES;

#define PATCH_COUNT (sizeof(PATCHES) / sizeof(PATCHES[0]))

static uintptr_t page_mask;
static int probe_fd[2] = {-1, -1};
static pthread_mutex_t probe_lock = PTHREAD_MUTEX_INITIALIZER;
static uintptr_t image_limit;
static uint32_t kill_signal_offsets[KILL_SITE_LIMIT];
static size_t kill_signal_count;

static uint64_t now_ms(void) {
    struct timespec spec;
    clock_gettime(CLOCK_MONOTONIC, &spec);
    return (uint64_t)spec.tv_sec * 1000 + (uint64_t)spec.tv_nsec / 1000000;
}

static void sleep_us(long microseconds) {
    struct timespec spec = {microseconds / 1000000, (microseconds % 1000000) * 1000};
    nanosleep(&spec, NULL);
}

static int unprotect(void *at) {
    void *page = (void *)((uintptr_t)at & page_mask);
    if (mprotect(page, 1, PROT_READ | PROT_WRITE | PROT_EXEC) == 0) return 1;

    LOG_ERROR("cannot make %p writable: %s", page, strerror(errno));
    return 0;
}

static void reprotect(void *at, size_t bytes) {
    void *page = (void *)((uintptr_t)at & page_mask);
    if (mprotect(page, 1, PROT_READ | PROT_EXEC) != 0) {
        LOG_ERROR("cannot restore %p to read-execute: %s", page, strerror(errno));
    }
    __builtin___clear_cache((char *)at, (char *)at + bytes);
}

// Concurrent unmapping of candidate regions
static int read_bytes(uintptr_t at, void *out, size_t bytes) {
    pthread_mutex_lock(&probe_lock);
    ssize_t written = write(probe_fd[1], (const void *)at, bytes);
    int complete = written > 0 && read(probe_fd[0], out, (size_t)written) == written && (size_t)written == bytes;
    pthread_mutex_unlock(&probe_lock);

    return complete;
}

static int read_words(uintptr_t at, uint32_t *words, size_t count) {
    return read_bytes(at, words, count * sizeof(*words));
}

static int is_image(uintptr_t base) {
    uint32_t anchor_a[2];
    uint32_t anchor_b;

    return read_words(base + ANCHOR_A_OFFSET, anchor_a, 2) && anchor_a[0] == ANCHOR_A_FIRST &&
           anchor_a[1] == ANCHOR_A_SECOND && read_words(base + ANCHOR_B_OFFSET, &anchor_b, 1) &&
           anchor_b == ANCHOR_B_WORD;
}

static uint32_t *find_image_in_region(uintptr_t low, uintptr_t high) {
    for (uintptr_t at = low; at + IMAGE_MIN_SIZE <= high; at += 0x1000) {
        if (is_image(at)) {
            image_limit = high;
            return (uint32_t *)at;
        }
    }
    return NULL;
}

static void apply_patches(uint32_t *base) {
    for (size_t index = 0; index < PATCH_COUNT; index++) {
        uint32_t word;
        if (!read_words((uintptr_t)base + PATCHES[index].offset, &word, 1)) return;
        if (word != PATCHES[index].expected && word != PATCHES[index].replacement) {
            LOG_ERROR("Unsupported packer build at +%#x (%08x)", PATCHES[index].offset, word);
            return;
        }
    }

    for (size_t index = 0; index < PATCH_COUNT; index++) {
        uint32_t *at = base + PATCHES[index].offset / 4;
        uint32_t word;
        if (!read_words((uintptr_t)at, &word, 1) || word == PATCHES[index].replacement) continue;
        if (word != PATCHES[index].expected || !unprotect(at)) continue;

        *(volatile uint32_t *)at = PATCHES[index].replacement;
        LOG("disabled %s at +%#x", PATCHES[index].checkName, PATCHES[index].offset);
        reprotect(at, sizeof(*at));
    }
}

static int precedes_kill_syscall(const code_unit *window, size_t index) {
    for (size_t ahead = 1; ahead <= KILL_LOOKAHEAD; ahead++) {
        if (memcmp(window + index + ahead, KILL_SYSCALL, sizeof(KILL_SYSCALL)) == 0) return 1;
    }
    return 0;
}

static void clear_kill_signals(uint32_t *base) {
    if (!image_limit || (uintptr_t)base >= image_limit) return;

    static code_unit window[SCAN_WINDOW];
    code_unit *units = (code_unit *)base;
    size_t total = (image_limit - (uintptr_t)base) / sizeof(*units);
    size_t at = 0;

    kill_signal_count = 0;
    while (at < total) {
        size_t want = total - at;
        if (want > SCAN_WINDOW) want = SCAN_WINDOW;
        if (want <= KILL_LOOKAHEAD + KILL_SYSCALL_UNITS - 1) break;

        if (!read_bytes((uintptr_t)(units + at), window, want * sizeof(*window))) {
            at += SCAN_CHUNK;
            continue;
        }

        size_t scan = want - KILL_LOOKAHEAD - KILL_SYSCALL_UNITS + 1;
        for (size_t index = 0; index < scan; index++) {
            if (window[index] != LOAD_SIGKILL || !precedes_kill_syscall(window, index)) continue;

            code_unit *site = units + at + index;
            if (!unprotect(site)) continue;

            *(volatile code_unit *)site = LOAD_SIGNAL_0;
            reprotect(site, sizeof(*site));

            if (kill_signal_count < KILL_SITE_LIMIT) {
                kill_signal_offsets[kill_signal_count++] = (uint32_t)((at + index) * sizeof(*units));
            }
        }

        at += scan;
    }

    LOG("cleared SIGKILL at %zu call sites", kill_signal_count);
}

static void reclear_kill_signals(uint32_t *base) {
    for (size_t index = 0; index < kill_signal_count; index++) {
        code_unit *site = (code_unit *)((uintptr_t)base + kill_signal_offsets[index]);
        code_unit unit;
        if (!read_bytes((uintptr_t)site, &unit, sizeof(unit)) || unit != LOAD_SIGKILL) continue;
        if (!unprotect(site)) continue;

        *(volatile code_unit *)site = LOAD_SIGNAL_0;
        reprotect(site, sizeof(*site));
    }
}

static uintptr_t parse_hex(const char **cursor) {
    uintptr_t value = 0;
    const char *at = *cursor;

    for (; *at; at++) {
        unsigned digit;
        if (*at >= '0' && *at <= '9') {
            digit = (unsigned)(*at - '0');
        } else if (*at >= 'a' && *at <= 'f') {
            digit = (unsigned)(*at - 'a') + 10;
        } else {
            break;
        }
        value = value * 16 + digit;
    }

    *cursor = at;
    return value;
}

static uint32_t *image_in_line(const char *line) {
    const char *cursor = line;

    uintptr_t low = parse_hex(&cursor);
    if (*cursor != '-') return NULL;

    cursor++;
    uintptr_t high = parse_hex(&cursor);
    if (*cursor != ' ' || high - low < REGION_MIN_SIZE || high - low > REGION_MAX_SIZE) return NULL;
    if (strncmp(++cursor, "r-x", 3) != 0) return NULL;

    for (int field = 0; field < 4 && *cursor; field++) {
        while (*cursor == ' ') cursor++;
        while (*cursor && *cursor != ' ') cursor++;
    }
    while (*cursor == ' ') cursor++;
    if (*cursor && *cursor != '[') return NULL;

    return find_image_in_region(low, high);
}

// Stdio deadlock risk from inherited locks after the watchdog fork
static void scan_maps(int (*visit)(const char *line, void *context), void *context) {
    int fd = open("/proc/self/maps", O_RDONLY | O_CLOEXEC);
    if (fd < 0) return;

    char buffer[MAPS_BUFFER_SIZE];
    size_t filled = 0;
    int done = 0;

    while (!done) {
        ssize_t got = read(fd, buffer + filled, sizeof(buffer) - filled - 1);
        if (got <= 0) break;

        filled += (size_t)got;
        buffer[filled] = '\0';

        char *line = buffer;
        for (char *end; (end = strchr(line, '\n')) != NULL; line = end + 1) {
            *end = '\0';
            done = visit(line, context);
            if (done) break;
        }

        filled -= (size_t)(line - buffer);
        memmove(buffer, line, filled);
    }

    close(fd);
}

static int visit_image_line(const char *line, void *context) {
    uint32_t **image = context;
    *image = image_in_line(line);
    return *image != NULL;
}

static uint32_t *find_image(void) {
    uint32_t *image = NULL;
    scan_maps(visit_image_line, &image);
    return image;
}

struct dex_copies {
    uint32_t file_size;
    uint32_t map_offset;
    uintptr_t bases[DEX_COPY_LIMIT];
    size_t count;
};

static int is_dex_copy(uintptr_t at, const struct dex_copies *copies) {
    uint8_t header[DEX_HEADER_BYTES];
    if (!read_bytes(at, header, sizeof(header)) || memcmp(header, DEX_MAGIC, 4) != 0) return 0;

    uint32_t file_size;
    uint32_t map_offset;
    memcpy(&file_size, header + DEX_FILE_SIZE_OFFSET, sizeof(file_size));
    memcpy(&map_offset, header + DEX_MAP_OFF_OFFSET, sizeof(map_offset));
    return file_size == copies->file_size && map_offset == copies->map_offset;
}

static int visit_dex_line(const char *line, void *context) {
    struct dex_copies *copies = context;
    const char *cursor = line;

    uintptr_t low = parse_hex(&cursor);
    if (*cursor != '-') return 0;

    cursor++;
    uintptr_t high = parse_hex(&cursor);
    if (*cursor != ' ' || cursor[1] != 'r') return 0;

    uintptr_t page_size = ~page_mask + 1;
    uintptr_t last = strstr(cursor, DEX_DATA_REGION) ? high : low + page_size;
    for (uintptr_t page = low; page < last && page < high; page += page_size) {
        if (!is_dex_copy(page, copies)) continue;

        copies->bases[copies->count++] = page;
        if (copies->count == DEX_COPY_LIMIT) return 1;
    }
    return 0;
}

static int make_writable(uint8_t *at, size_t bytes) {
    uintptr_t first = (uintptr_t)at & page_mask;
    uintptr_t end = (uintptr_t)at + bytes;
    if (mprotect((void *)first, end - first, PROT_READ | PROT_WRITE) == 0) return 1;

    LOG_ERROR("cannot make %p+%zu writable: %s", (void *)at, bytes, strerror(errno));
    return 0;
}

JNIEXPORT jint JNICALL Java_hx_Boot_writeCode(JNIEnv *env, jclass type, jbyteArray records) {
    (void)type;

    jsize length = (*env)->GetArrayLength(env, records);
    jbyte *bytes = (*env)->GetByteArrayElements(env, records, NULL);
    if (!bytes) return -1;

    struct dex_copies copies = {0};
    int written = 0;
    int differed = 0;
    size_t total = (size_t)length;
    size_t at = 0;

    while (at + CODE_RECORD_HEADER_BYTES <= total) {
        uint32_t record[4];
        memcpy(record, bytes + at, sizeof(record));
        at += CODE_RECORD_HEADER_BYTES;

        uint32_t file_size = record[0];
        uint32_t map_offset = record[1];
        uint32_t offset = record[2];
        uint32_t size = record[3];
        if (size > total - at || offset > file_size || size > file_size - offset) {
            LOG_ERROR("invalid code record at byte %zu: offset %#x, %u bytes", at, offset, size);
            break;
        }

        if (copies.file_size != file_size || copies.map_offset != map_offset) {
            memset(&copies, 0, sizeof(copies));
            copies.file_size = file_size;
            copies.map_offset = map_offset;
            scan_maps(visit_dex_line, &copies);
            if (!copies.count) LOG_ERROR("no loaded dex of %u bytes with map at %#x", file_size, map_offset);
        }

        for (size_t copy = 0; copy < copies.count; copy++) {
            uint8_t *target = (uint8_t *)copies.bases[copy] + offset;
            if (!make_writable(target, size)) continue;

            if (memcmp(target, bytes + at, size) != 0) differed++;
            memcpy(target, bytes + at, size);
            written++;
        }

        at += size;
    }

    (*env)->ReleaseByteArrayElements(env, records, bytes, JNI_ABORT);
    LOG("code overlay: %d code items written, %d differed from the loaded code", written, differed);
    return written;
}

static void *patch_image(void *unused) {
    (void)unused;

    uint32_t *image = NULL;
    uint64_t deadline = now_ms() + SEARCH_TIMEOUT_MS;
    while (!image && now_ms() < deadline) {
        image = find_image();
        if (!image) sleep_us(SEARCH_INTERVAL_US);
    }

    if (!image) {
        LOG_ERROR("packer image search timed out");
        return NULL;
    }

    LOG("packer image at %p", (void *)image);
    apply_patches(image);
    clear_kill_signals(image);

    deadline = now_ms() + REPATCH_TIMEOUT_MS;
    while (now_ms() < deadline) {
        sleep_us(REPATCH_INTERVAL_US);
        if (find_image() != image) continue;
        apply_patches(image);
        reclear_kill_signals(image);
    }

    return NULL;
}

// pthread_create deadlock risk under the linker lock in a constructor
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)vm;
    (void)reserved;

    if (pipe2(probe_fd, O_CLOEXEC | O_NONBLOCK) != 0) {
        LOG_ERROR("cannot open the probe pipe: %s", strerror(errno));
        return JNI_VERSION_1_6;
    }

    long page_size = sysconf(_SC_PAGESIZE);
    page_mask = ~(uintptr_t)((page_size > 0 ? (uintptr_t)page_size : 0x1000) - 1);

    pthread_t thread;
    int error = pthread_create(&thread, NULL, patch_image, NULL);
    if (error == 0) {
        pthread_detach(thread);
    } else {
        LOG_ERROR("cannot start the patch thread: %s", strerror(error));
    }

    return JNI_VERSION_1_6;
}
