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

#define MOV_W0_0 0x52800000
#define MOV_W0_1 0x52800020
#define RET 0xd65f03c0
#define NOP 0xd503201f

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

static uint64_t now_ms(void) {
    struct timespec spec;
    clock_gettime(CLOCK_MONOTONIC, &spec);
    return (uint64_t)spec.tv_sec * 1000 + (uint64_t)spec.tv_nsec / 1000000;
}

static void sleep_us(long microseconds) {
    struct timespec spec = {microseconds / 1000000, (microseconds % 1000000) * 1000};
    nanosleep(&spec, NULL);
}

static int unprotect(uint32_t *at) {
    void *page = (void *)((uintptr_t)at & page_mask);
    if (mprotect(page, 1, PROT_READ | PROT_WRITE | PROT_EXEC) == 0) return 1;

    LOG_ERROR("cannot make %p writable: %s", page, strerror(errno));
    return 0;
}

static void reprotect(uint32_t *at) {
    void *page = (void *)((uintptr_t)at & page_mask);
    if (mprotect(page, 1, PROT_READ | PROT_EXEC) != 0) {
        LOG_ERROR("cannot restore %p to read-execute: %s", page, strerror(errno));
    }
    __builtin___clear_cache((char *)at, (char *)(at + 1));
}

// Concurrent unmapping of candidate regions
static int read_words(uintptr_t at, uint32_t *words, size_t count) {
    size_t bytes = count * sizeof(*words);

    ssize_t written = write(probe_fd[1], (const void *)at, bytes);
    if (written <= 0) return 0;

    return read(probe_fd[0], words, (size_t)written) == written && (size_t)written == bytes;
}

static int write_word(uintptr_t at, uint32_t word) {
    if (write(probe_fd[1], &word, sizeof(word)) != (ssize_t)sizeof(word)) return 0;
    if (read(probe_fd[0], (void *)at, sizeof(word)) == (ssize_t)sizeof(word)) return 1;

    // A faulted store leaves the word queued
    uint32_t drain;
    while (read(probe_fd[0], &drain, sizeof(drain)) > 0) continue;
    return 0;
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
        if (is_image(at)) return (uint32_t *)at;
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

        if (write_word((uintptr_t)at, PATCHES[index].replacement)) {
            LOG("disabled %s at +%#x", PATCHES[index].checkName, PATCHES[index].offset);
        }
        reprotect(at);
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
static uint32_t *find_image(void) {
    int fd = open("/proc/self/maps", O_RDONLY | O_CLOEXEC);
    if (fd < 0) return NULL;

    static char buffer[MAPS_BUFFER_SIZE];
    uint32_t *image = NULL;
    size_t filled = 0;

    while (!image) {
        ssize_t got = read(fd, buffer + filled, sizeof(buffer) - filled - 1);
        if (got <= 0) break;

        filled += (size_t)got;
        buffer[filled] = '\0';

        char *line = buffer;
        for (char *end; (end = strchr(line, '\n')) != NULL; line = end + 1) {
            *end = '\0';
            image = image_in_line(line);
            if (image) break;
        }

        filled -= (size_t)(line - buffer);
        memmove(buffer, line, filled);
    }

    close(fd);
    return image;
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

    deadline = now_ms() + REPATCH_TIMEOUT_MS;
    while (now_ms() < deadline) {
        sleep_us(REPATCH_INTERVAL_US);
        if (find_image() == image) apply_patches(image);
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
