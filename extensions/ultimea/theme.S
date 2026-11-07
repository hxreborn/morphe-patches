.text
.global theme_hooks
theme_hooks:
 .word color_transform-theme_hooks
 .word gradient_transform-theme_hooks
 .word color_instruction-theme_hooks
 .word gradient_instruction-theme_hooks
 .word mode-theme_hooks
 .word callers-theme_hooks
image_offset: .word 0
cache_address: .word 0
mode: .word 1
 .word surface_transform-theme_hooks
 .word surface_instruction-theme_hooks
 .word surface_input-theme_hooks
 .word surface_output-theme_hooks
 .word surface_restore-theme_hooks
color_transform:
 sub x15, x15, #128
 stp x5, x6, [x15]
 stp x7, x8, [x15, #16]
 stp x16, x17, [x15, #32]
 str x30, [x15, #48]
 stp x1, x2, [x15, #64]
 stp x3, x4, [x15, #80]
 mrs x16, nzcv
 str x16, [x15, #96]
 bl read_mode
 adr x2, theme_hooks
 ldr w16, image_offset
 sub x2, x2, x16
 ldr x1, [x29, #8]
 sub x1, x1, x2
 adr x4, callers
 mov x6, x29
 mov x5, #2
 mov w7, #5
search_frame:
 mov x2, x4
next_caller:
 ldr w16, [x2], #4
 cbz w16, parent_frame
 lsr w17, w16, #3
 lsl w17, w17, #2
 cmp w1, w17
 b.ne next_caller
 and w7, w16, #7
 cmp w7, #7
 b.ne apply_color
 mov w7, #5
 mov x5, #2
 b parent_frame
parent_frame:
 subs x5, x5, #1
 b.eq apply_color
 ldr x16, [x6]
 cmp x16, x6
 b.ls apply_color
 tst x16, #7
 b.ne apply_color
 mov x6, x16
 ldr x1, [x6, #8]
 adr x2, theme_hooks
 ldr w16, image_offset
 sub x2, x2, x16
 sub x1, x1, x2
 b search_frame
apply_color:
 adr x2, palette
next_palette:
 ldp w16, w17, [x2], #8
 cbz w16, color_done
 cmp w16, w7
 b.ne skip_palette
 cmp w17, w0
 b.eq replace_color
skip_palette:
 add x2, x2, #8
 b next_palette
replace_color:
 adr x1, theme_hooks
 ldr w16, image_offset
 sub x1, x1, x16
 ldr w16, cache_address
 ldr w16, [x1, x16]
 sub w16, w16, #1
 cmp w16, #2
 ldr w0, [x2]
 b.ne color_done
 ldr w0, [x2, #4]
color_done:
 ldr x16, [x15, #96]
 msr nzcv, x16
 ldp x1, x2, [x15, #64]
 ldp x3, x4, [x15, #80]
 ldp x5, x6, [x15]
 ldp x7, x8, [x15, #16]
 ldp x16, x17, [x15, #32]
 ldr x30, [x15, #48]
 add x15, x15, #128
color_instruction:
 asr x1, x0, #24
 ret
gradient_transform:
 sub x15, x15, #224
 stp x1, x2, [x15]
 stp x3, x4, [x15, #16]
 stp x5, x6, [x15, #32]
 stp x7, x8, [x15, #48]
 stp x16, x17, [x15, #64]
 str x30, [x15, #80]
 mrs x16, nzcv
 str x16, [x15, #88]
 stp q0, q1, [x15, #96]
 stp q2, q3, [x15, #128]
 stp q4, q5, [x15, #160]
 stp q6, q7, [x15, #192]
gradient_instruction:
 mov x0, x2
 bl read_mode
 ldur w1, [x0, #0x13]
 lsr x1, x1, #1
 cmp x1, #8
 b.lt gradient_done
 cmp x1, #256
 b.gt gradient_done
 add x2, x0, #0x17
 mov x3, x2
 lsr x4, x1, #2
 fmov s6, #1.0
 ldr s4, neutral_floor
 ldr s5, neutral_spread
check_stop:
 ldp s0, s1, [x3, #4]
 ldr s2, [x3, #12]
 fmin s3, s0, s1
 fmin s3, s3, s2
 fmax s7, s0, s1
 fmax s7, s7, s2
 fsub s7, s7, s3
 fcmp s7, s5
 b.gt gradient_done
 fcmp s3, s4
 b.ge light_stop
 ldr s7, transformed_floor
 fcmp s3, s7
 b.lt gradient_done
 ldr s7, transformed_ceiling
 fmax s3, s0, s1
 fmax s3, s3, s2
 fcmp s3, s7
 b.gt gradient_done
 b next_stop
light_stop:
 fmin s6, s6, s3
next_stop:
 add x3, x3, #16
 subs x4, x4, #1
 b.ne check_stop
 ldr s4, page_floor
 fcmp s6, s4
 b.lt track_stops
 adr x2, theme_hooks
 ldr w16, image_offset
 sub x2, x2, x16
 ldr w16, cache_address
 ldr w16, [x2, x16]
 sub w16, w16, #1
 add x2, x0, #0x17
 cmp w16, #2
 ldr s4, page_tone
 b.ne paint_stops
 fmov s4, wzr
 b paint_stops
track_stops:
 ldr s4, track_tone
paint_stops:
 lsr x4, x1, #2
paint_stop:
 stp s4, s4, [x2, #4]
 str s4, [x2, #12]
 add x2, x2, #16
 subs x4, x4, #1
 b.ne paint_stop
gradient_done:
 ldr x16, [x15, #88]
 msr nzcv, x16
 ldp x1, x2, [x15]
 ldp x3, x4, [x15, #16]
 ldp x5, x6, [x15, #32]
 ldp x7, x8, [x15, #48]
 ldp x16, x17, [x15, #64]
 ldr x30, [x15, #80]
 ldp q0, q1, [x15, #96]
 ldp q2, q3, [x15, #128]
 ldp q4, q5, [x15, #160]
 ldp q6, q7, [x15, #192]
 add x15, x15, #224
 ret
read_mode:
 adr x1, theme_hooks
 ldr w16, image_offset
 sub x1, x1, x16
 ldr w16, cache_address
 ldr w16, [x1, x16]
 cbnz w16, mode_ready
 sub x15, x15, #160
 stp x0, x30, [x15]
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
 add x4, x15, #128
 mov w5, #10
user_digit:
 udiv w1, w0, w5
 msub w3, w1, w5, w0
 add w3, w3, #48
 strb w3, [x4], #1
 mov w0, w1
 cbnz w0, user_digit
 add x5, x15, #128
copy_user:
 ldrb w3, [x4, #-1]!
 strb w3, [x2], #1
 cmp x4, x5
 b.ne copy_user
 adr x1, path_suffix
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
 b.lt cache_default
 str x0, [x15, #16]
 add x1, x15, #24
 mov x2, #1
 mov x8, #63
 svc #0
 str x0, [x15, #128]
 ldr x0, [x15, #16]
 mov x8, #57
 svc #0
 ldr x0, [x15, #128]
 cmp x0, #1
 b.ne cache_default
 ldrb w16, [x15, #24]
 cmp w16, #1
 b.lt cache_default
 cmp w16, #2
 b.le cache_mode
cache_default:
 ldr w16, mode
cache_mode:
 add w16, w16, #1
 adr x1, theme_hooks
 ldr w2, image_offset
 sub x1, x1, x2
 ldr w2, cache_address
 str w16, [x1, x2]
 ldr x0, [x15]
 ldr x30, [x15, #8]
 add x15, x15, #160
mode_ready:
 sub w16, w16, #1
 ret
user_uid_range: .word 100000
path_prefix: .asciz "/data/user/"
path_suffix: .asciz "/com.dynanic.ultimea/files/hx_ultimea_theme"
.balign 4
transformed_floor: .float 0.0
transformed_ceiling: .float 0.10
neutral_floor: .float 0.84
neutral_spread: .float 0.08
page_floor: .float 0.94
page_tone: .float 0.07058823529411765
track_tone: .float 0.21176470588235294

.balign 4
surface_transform:
 sub x15, x15, #224
 stp x0, x1, [x15]
 stp x2, x3, [x15, #16]
 stp x4, x5, [x15, #32]
 stp x6, x7, [x15, #48]
 stp x8, x16, [x15, #64]
 stp x17, x30, [x15, #80]
 mrs x16, nzcv
 str x16, [x15, #104]
 stp q0, q1, [x15, #112]
 stp q2, q3, [x15, #144]
 stp q4, q5, [x15, #176]
surface_input:
 mov x0, x0
 str x0, [x15, #208]
 bl surface_color
 str x0, [x15, #96]
 ldr x16, [x15, #104]
 msr nzcv, x16
 ldp x0, x1, [x15]
 ldp x2, x3, [x15, #16]
 ldp x4, x5, [x15, #32]
 ldp x6, x7, [x15, #48]
 ldp x8, x16, [x15, #64]
 ldp x17, x30, [x15, #80]
 ldp q0, q1, [x15, #112]
 ldp q2, q3, [x15, #144]
 ldp q4, q5, [x15, #176]
surface_output:
 ldr x0, [x15, #96]
surface_instruction:
 stur w0, [x2, #23]
surface_restore:
 ldr x0, [x15, #208]
 add x15, x15, #224
 ret
surface_color:
 cmp w0, w22
 b.eq surface_done
 ldur d0, [x0, #7]
 fmov d1, #1.0
 fcmp d0, d1
 b.ne surface_done
 ldur d0, [x0, #15]
 ldur d1, [x0, #23]
 ldur d2, [x0, #31]
 fmin d3, d0, d1
 fmin d3, d3, d2
 ldr d4, surface_floor
 fcmp d3, d4
 b.lt surface_done
 fmax d4, d0, d1
 fmax d4, d4, d2
 fsub d4, d4, d3
 ldr d5, surface_spread
 fcmp d4, d5
 b.gt surface_done
 adr x1, theme_hooks
 ldr w16, image_offset
 sub x1, x1, x16
 ldr w16, cache_address
 add w16, w16, #4
 ldr w2, [x1, x16]
 cmp w2, #16
 b.lt find_black
 tst w2, #7
 b.ne find_black
 ldr x6, [x27, #8]
 ldr w8, maximum_pool_entries
 cmp x6, x8
 b.hi surface_done
 lsr x3, x2, #3
 sub x3, x3, #2
 cmp x3, x6
 b.hs find_black
 add x5, x27, #16
 add x7, x5, x6, lsl #3
 ldrb w8, [x7, x3]
 and w8, w8, #15
 cmp w8, #1
 b.ne find_black
 ldr x2, [x27, x2]
 tbz x2, #0, find_black
 ldur w16, [x2, #-1]
 ubfx w16, w16, #8, #4
 cmp w16, #3
 b.ne find_black
 ldur w3, [x0, #39]
 ldur w4, [x2, #39]
 cmp w3, w4
 b.ne find_black
 ldur d0, [x2, #7]
 fmov d1, #1.0
 fcmp d0, d1
 b.ne find_black
 ldur d0, [x2, #15]
 fcmp d0, #0.0
 b.ne find_black
 ldur d0, [x2, #23]
 fcmp d0, #0.0
 b.ne find_black
 ldur d0, [x2, #31]
 fcmp d0, #0.0
 b.ne find_black
 mov x0, x2
 ret
find_black:
 ldur w3, [x0, #39]
 ldr x6, [x27, #8]
 cmp x6, #1
 b.lt surface_done
 ldr w8, maximum_pool_entries
 cmp x6, x8
 b.gt surface_done
 add x5, x27, #16
 add x7, x5, x6, lsl #3
 mov x4, #0
next_pool_color:
 ldrb w8, [x7, x4]
 and w8, w8, #15
 cmp w8, #1
 b.ne skip_pool_color
 ldr x2, [x5, x4, lsl #3]
 tbz x2, #0, skip_pool_color
 ldur w16, [x2, #-1]
 ubfx w16, w16, #8, #4
 cmp w16, #3
 b.ne skip_pool_color
 ldur w16, [x2, #39]
 cmp w16, w3
 b.ne skip_pool_color
 ldur d0, [x2, #7]
 fmov d1, #1.0
 fcmp d0, d1
 b.ne skip_pool_color
 ldur d0, [x2, #15]
 fcmp d0, #0.0
 b.ne skip_pool_color
 ldur d0, [x2, #23]
 fcmp d0, #0.0
 b.ne skip_pool_color
 ldur d0, [x2, #31]
 fcmp d0, #0.0
 b.ne skip_pool_color
 lsl w8, w4, #3
 add w8, w8, #16
 adr x1, theme_hooks
 ldr w16, image_offset
 sub x1, x1, x16
 ldr w16, cache_address
 add w16, w16, #4
 str w8, [x1, x16]
 mov x0, x2
 ret
skip_pool_color:
 add x4, x4, #1
 cmp x4, x6
 b.lt next_pool_color
surface_done:
 ret
surface_floor: .double 0.84
surface_spread: .double 0.08
maximum_pool_entries: .word 1000000
.balign 4
palette:
 .word 1, 0xff161616, 0xffececee, 0xffececee
 .word 1, 0xff646464, 0xffb8bcc2, 0xffb8bcc2
 .word 1, 0xff999999, 0xffaab0b5, 0xffaab0b5
 .word 1, 0xff541597, 0xffc499ff, 0xffc499ff
 .word 1, 0xff191919, 0xffececee, 0xffececee
 .word 1, 0xff000000, 0xffececee, 0xffececee
 .word 1, 0xffb2b2b2, 0xffb8bcc2, 0xffb8bcc2
 .word 1, 0xffaab0b5, 0xffb8bcc2, 0xffb8bcc2
 .word 2, 0xffffffff, 0xff24242a, 0xff24242a
 .word 2, 0xfff9f9f9, 0xff121216, 0xff121216
 .word 2, 0xfff6f6f6, 0xff121216, 0xff121216
 .word 2, 0xffefefef, 0xff121216, 0xff121216
 .word 2, 0xffe9e9e9, 0xff36363e, 0xff36363e
 .word 2, 0xffe7e7e7, 0xff36363e, 0xff36363e
 .word 2, 0xffd8d9db, 0xff45454d, 0xff45454d
 .word 3, 0xffffffff, 0xff45454d, 0xff45454d
 .word 3, 0xfff9f9f9, 0xff36363e, 0xff36363e
 .word 3, 0xffe9e9e9, 0xff36363e, 0xff36363e
 .word 3, 0xffe7e7e7, 0xff36363e, 0xff36363e
 .word 3, 0xff646464, 0xff73737d, 0xff73737d
 .word 3, 0xff161616, 0xff45454d, 0xff45454d
 .word 3, 0xffd8d9db, 0xff45454d, 0xff45454d
 .word 3, 0xff541597, 0xffb585ee, 0xffb585ee
 .word 5, 0xff000000, 0xffececee, 0xffececee
 .word 5, 0xff541597, 0xffc499ff, 0xffc499ff
 .word 5, 0xff161616, 0xffececee, 0xffececee
 .word 5, 0xff646464, 0xffb8bcc2, 0xffb8bcc2
 .word 5, 0xfff9f9f9, 0xff121216, 0xff000000
 .word 5, 0xfff6f6f6, 0xff121216, 0xff000000
 .word 5, 0xffe9e9e9, 0xff36363e, 0xff36363e
 .word 5, 0xffe7e7e7, 0xff36363e, 0xff36363e
 .word 6, 0xffffffff, 0xff121216, 0xff000000
 .word 6, 0xfff9f9f9, 0xff121216, 0xff000000
 .word 6, 0xfff6f6f6, 0xff121216, 0xff000000
 .word 6, 0xffefefef, 0xff121216, 0xff000000
 .word 0, 0, 0, 0
callers:
