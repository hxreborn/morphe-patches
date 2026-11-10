.text
.global accent_hooks
accent_hooks:
 .word paint_transform-accent_hooks
 .word packed_transform-accent_hooks
 .word gradient_transform-accent_hooks
image_offset: .word 0
cache_address: .word 0
palette_count: .word 0
 .word source_colors-accent_hooks
 .word paint_instruction-accent_hooks
 .word packed_instruction-accent_hooks
 .word gradient_instruction-accent_hooks
 .word paint_pointer-accent_hooks
.macro save_registers
 sub x15, x15, #176
 stp x0, x1, [x15]
 stp x2, x3, [x15, #16]
 stp x4, x5, [x15, #32]
 stp x6, x7, [x15, #48]
 stp x8, x9, [x15, #64]
 stp x16, x17, [x15, #80]
 str x30, [x15, #96]
 mrs x16, nzcv
 str x16, [x15, #104]
 stp q0, q1, [x15, #112]
 stp q2, q3, [x15, #144]
.endm
.macro restore_registers
 ldr x16, [x15, #104]
 msr nzcv, x16
 ldp x0, x1, [x15]
 ldp x2, x3, [x15, #16]
 ldp x4, x5, [x15, #32]
 ldp x6, x7, [x15, #48]
 ldp x8, x9, [x15, #64]
 ldp x16, x17, [x15, #80]
 ldr x30, [x15, #96]
 ldp q0, q1, [x15, #112]
 ldp q2, q3, [x15, #144]
 add x15, x15, #176
.endm
paint_transform:
 save_registers
paint_pointer:
 add x0, x2, x1
 ldr w1, [x0]
 cbnz w1, paint_done
 sub x0, x0, #16
 bl map_float_rgb
paint_done:
 restore_registers
paint_instruction:
 mov x0, x22
 ret
packed_transform:
 save_registers
packed_instruction:
 mov x0, x2
 bl map_packed_rgb
 str x0, [x15]
 restore_registers
 ret
gradient_transform:
 save_registers
 ldur w1, [x0, #19]
 lsr w1, w1, #1
 cmp w1, #8
 b.lt gradient_done
 cmp w1, #256
 b.gt gradient_done
 tst w1, #3
 b.ne gradient_done
 add x0, x0, #27
 lsr w1, w1, #2
 sub x15, x15, #32
 stp x0, x1, [x15]
 str x30, [x15, #16]
gradient_stop:
 ldr x0, [x15]
 bl map_float_rgb
 ldp x0, x1, [x15]
 add x0, x0, #16
 subs x1, x1, #1
 stp x0, x1, [x15]
 b.ne gradient_stop
 ldr x30, [x15, #16]
 add x15, x15, #32
gradient_done:
 restore_registers
gradient_instruction:
 mov x15, x29
 ret
map_packed_rgb:
 sub x15, x15, #32
 stp x0, x30, [x15]
 bl palette_entry
 cbz x0, packed_unchanged
 ldr w1, [x0]
 ldr w2, [x15]
 and w1, w1, #0x00ffffff
 and w2, w2, #0xff000000
 orr w0, w1, w2
 b packed_return
packed_unchanged:
 ldr x0, [x15]
packed_return:
 ldr x30, [x15, #8]
 add x15, x15, #32
 ret
map_float_rgb:
 sub x15, x15, #32
 stp x0, x30, [x15]
 ldp s0, s1, [x0]
 ldr s2, [x0, #8]
 ldr s3, channel_scale
 fmul s0, s0, s3
 fmul s1, s1, s3
 fmul s2, s2, s3
 fcvtau w1, s0
 fcvtau w2, s1
 fcvtau w3, s2
 orr w4, w1, w2
 orr w4, w4, w3
 cmp w4, #255
 b.hi float_return
 lsl w0, w1, #16
 orr w0, w0, w2, lsl #8
 orr w0, w0, w3
 bl palette_entry
 cbz x0, float_return
 ldr w4, [x0]
 ubfx w1, w4, #16, #8
 ubfx w2, w4, #8, #8
 and w3, w4, #255
 ucvtf s0, w1
 ucvtf s1, w2
 ucvtf s2, w3
 ldr s3, channel_scale
 fdiv s0, s0, s3
 fdiv s1, s1, s3
 fdiv s2, s2, s3
 ldr x1, [x15]
 stp s0, s1, [x1]
 str s2, [x1, #8]
float_return:
 ldp x0, x30, [x15]
 add x15, x15, #32
 ret
palette_entry:
 sub x15, x15, #32
 stp x0, x30, [x15]
 bl read_palette
 cbz x0, palette_return
 str x0, [x15, #16]
 ldr w0, [x15]
 and w0, w0, #0x00ffffff
 adr x1, source_colors
 ldr w2, palette_count
 mov w3, #0
palette_next:
 cmp w3, w2
 b.ge palette_missing
 ldr w4, [x1], #4
 cmp w4, w0
 b.eq palette_found
 add w3, w3, #1
 b palette_next
palette_found:
 ldr x0, [x15, #16]
 add x0, x0, x3, lsl #4
 b palette_return
palette_missing:
 mov x0, #0
palette_return:
 ldr x30, [x15, #8]
 add x15, x15, #32
 ret
read_palette:
 adr x16, accent_hooks
 ldr w17, image_offset
 sub x16, x16, x17
 ldr w17, cache_address
 add x16, x16, x17
 ldar x0, [x16]
 add x17, x16, #8
 ldar x1, [x17]
 cbz x0, palette_refresh
 cbz x1, palette_refresh
 ldr x2, [x1]
 ldr x3, [x0]
 cmp x2, x3
 b.ne palette_refresh
 add x0, x0, #8
 ret
palette_refresh:
 sub x15, x15, #64
 stp x16, x30, [x15]
 ldr x0, [x16, #16]
 cbz x0, palette_load_control
 mov x0, #1
 add x1, x15, #32
 mov x8, #113
 svc #0
 cbnz x0, palette_previous
 ldr x16, [x15]
 ldr x0, [x16, #16]
 ldr x1, [x15, #32]
 cmp x1, x0
 b.lt palette_previous
palette_load_control:
 ldr x16, [x15]
 ldr x0, [x16, #8]
 cbnz x0, palette_load_colors
 adr x1, version_suffix
 mov x2, #8
 bl map_palette_file
 cbz x0, palette_retry
 ldr x16, [x15]
 add x1, x16, #8
 stlr x0, [x1]
palette_load_colors:
 adr x1, path_suffix
 ldr w2, palette_count
 lsl x2, x2, #4
 add x2, x2, #8
 bl map_palette_file
 cbz x0, palette_retry
 str x0, [x15, #16]
 ldr x16, [x15]
 ldr x1, [x16, #8]
 ldr x1, [x1]
 ldr x2, [x0]
 cmp x1, x2
 b.ne palette_discard
 ldr x3, [x16]
 cbz x3, palette_publish
 ldr x3, [x3]
 cmp x3, x2
 b.eq palette_discard
palette_publish:
 stlr x0, [x16]
 str xzr, [x16, #16]
 b palette_previous
palette_discard:
 ldr w1, palette_count
 lsl x1, x1, #4
 add x1, x1, #8
 mov x8, #215
 svc #0
palette_retry:
 mov x0, #1
 add x1, x15, #32
 mov x8, #113
 svc #0
 cbnz x0, palette_previous
 ldr x0, [x15, #32]
 add x0, x0, #1
 ldr x16, [x15]
 str x0, [x16, #16]
palette_previous:
 ldr x16, [x15]
 ldr x0, [x16]
 cbz x0, palette_read_return
 add x0, x0, #8
palette_read_return:
 ldr x30, [x15, #8]
 add x15, x15, #64
 ret
map_palette_file:
 sub x15, x15, #192
 stp x1, x2, [x15]
 str x30, [x15, #16]
 mov x8, #174
 svc #0
 ldr w1, user_uid_range
 udiv w0, w0, w1
 adr x1, path_prefix
 add x2, x15, #32
copy_prefix:
 ldrb w3, [x1], #1
 cbz w3, write_user
 strb w3, [x2], #1
 b copy_prefix
write_user:
 add x4, x15, #160
 mov w5, #10
user_digit:
 udiv w1, w0, w5
 msub w3, w1, w5, w0
 add w3, w3, #48
 strb w3, [x4], #1
 mov w0, w1
 cbnz w0, user_digit
 add x5, x15, #160
copy_user:
 ldrb w3, [x4, #-1]!
 strb w3, [x2], #1
 cmp x4, x5
 b.ne copy_user
 ldr x1, [x15]
copy_suffix:
 ldrb w3, [x1], #1
 strb w3, [x2], #1
 cbnz w3, copy_suffix
 mov x0, #-100
 add x1, x15, #32
 mov x2, #0
 mov x3, #0
 mov x8, #56
 svc #0
 cmp x0, #0
 b.lt palette_file_failed
 str x0, [x15, #24]
 mov x1, #0
 mov x2, #2
 mov x8, #62
 svc #0
 ldr x1, [x15, #8]
 cmp x0, x1
 b.lt palette_file_short
 ldr x4, [x15, #24]
 mov x0, #0
 mov x2, #1
 mov x3, #1
 mov x5, #0
 mov x8, #222
 svc #0
 cmp x0, #0
 b.ge palette_file_close
palette_file_short:
 mov x0, #0
palette_file_close:
 str x0, [x15]
 ldr x0, [x15, #24]
 mov x8, #57
 svc #0
 ldr x0, [x15]
 b palette_file_return
palette_file_failed:
 mov x0, #0
palette_file_return:
 ldr x30, [x15, #16]
 add x15, x15, #192
 ret
.p2align 2
channel_scale: .float 255.0
user_uid_range: .word 100000
path_prefix: .asciz "/data/user/"
path_suffix: .asciz "/com.dynanic.ultimea/files/hx_ultimea_accent"
version_suffix: .asciz "/com.dynanic.ultimea/files/hx_ultimea_accent_version"
.p2align 2
source_colors: .word 0
