/*
 * Copyright (C) 2024 The Android Open Source Project
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: Apache-2.0
 *
 * Adapted from material-components/material-components-android:
 * https://github.com/material-components/material-components-android/commit/66b0eeb53afeb6025ffbbd37734b08e5bce197ed
 * Commit 66b0eeb53afeb6025ffbbd37734b08e5bce197ed (2026-10-12),
 * lib/java/com/google/android/material/shape/MaterialShapes.java
 */
package app.hxreborn.extension.shared;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import android.util.Base64;

final class LoadingIndicatorShapes {

    static final int[] INDETERMINATE_CUBIC_COUNTS = { 58, 28, 22, 44, 48, 24, 48 };

    static final int DETERMINATE_CUBIC_COUNT = 50;

    static final int DETERMINATE_MORPH_INDEX = INDETERMINATE_CUBIC_COUNTS.length;

    static final int FLOATS_PER_CUBIC = 16;

    private static final String MORPHS = """
            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
            qeg+wZp4PxSVQD9gUYQ/TKnoPsGaeD8UlUA/U++YP0TJzj7Htbk/G11eP0eNrD87d7o+zdD8PyCyvD87K78/MQIQPtPsQj8kDZQ/Oyu/PzEC
            ED7T7EI/JA2UPzhK6j8uQ1I+1VeiPyTu+T81ahQ/K5SpPtbDAz8l9TE/Mok+Pyj4fD7YLmQ/Jx44PzKJPj8o+Hw+2C5kPyceOD8lukQ/G0P8
            Pt5/cD8tNdw/HRQyPw+NCz72aBQ/Nw0qPx4WDj8HL2w/CFteP0OAUj8eFg4/By9sPwhbXj9DgFI/Hk9TPwWcBz8LR0Y/RdioPx6ImD8EJw0/
            DjMsP0hIrz8ewd0/AtLQPxEfFT9KznY/HsHdPwLS0D8RHxU/Ss52Px+6Ez77cRo/HchoP1R9wz8gsko+7cJTPypxvD9cuIQ/IaqCPt287j83
            Gw4/Y0moPyGqgj7dvO4/NxsOP2NJqD8jMoo+yxQIP0sahj9q77A/DyfAPrU/Gj9Zugw/cFRaPvkoWD6eCvE/UipSP3Mjyz75KFg+ngrxP1Iq
            Uj9zI8s+3AqgPpWcbD9MPVA/dCk6Pr7s6D6M/4Y/RlBSP3TXWD6hzzQ+hEpbP0BjVD91KiA+oc80PoRKWz9AY1Q/dSogPolfOD5zP6o/O2oa
            P3WPhz5aplA+XaHMP0AAkD91a+U+PvuwPkgMYT9LPFo/dLfUPj77sD5IDGE/SzxaP3S31D4eNQA+NndKP1iLQj90JSQ9+tywPiTnzj9l2i4/
            czKcPblPUD4Tidw/cykWP3HcPT25T1A+E4ncP3MpFj9x3D09mNhLPgp90j95wF4/cSniPU6y2D4Bfzk/fc5oP3BccDzHvsM98SiQP39SXD9v
            c1Q8x77DPfEokD9/Ulw/b3NUvIw6Lz3U0RQ/gN3XP24A6L2B+BY9t9mRP3228D9s0mW9tfc4PZp8tz9zQLo/a+fPvbX3OD2afLc/c0C6P2vn
            z734UpA9dVqLP2Xm3D9q6ay+HVb4PTTNxT9YjQI/ajs9vj6EpDzn0Kk/SzMmP2nch74+hKQ859CpP0szJj9p3Ie+Wl14vCV5nj8//n4/aPb8
            volEML1IreA/O3Z8P2nlor6hqeC9sNz9P0B8Sj9sqHi+oangvbDc/T9AfEo/bKh4vqcVCr3GlD8/QZneP21xDL6sgDW93AryP0K3cT9uX1a+
            setfvfEo0D9D1QY/b3NUvrHrX73xKNA/Q9UGP29zVL7JZ1++IR2sP0iqtj9zlu++4ONevks9CT9NgGg/dX8HvvhfYL50qy4/UlYaP3Vgs774
            X2C+dKsuP1JWGj91YLO/Drtwvp58kj9Z+PI/dSvEvyLVPr7Bla4/S24YP27L0r8hYdC+3ZKjPzdtGj9jWv6/IWHQvt2Soz83bRo/Y1r+vyB1
            ob7tqkI/KrTNP1zHAL8fiXO++2gqPx38gz9Uhki/Hp1EvwLS1j8RRDo/Ss50vx6dRL8C0tY/EUQ6P0rOdL8eZl6/BCrUPw5PSD9IQYy/Hi92
            vwWkSj8LWlc/Rcrevx34kL8HPNI/CGVmP0Nsbr8d+JC/BzzSPwhlZj9DbG6/HQRBvw+eAj72ehw/NvwKvyW2nL8bV9E+3qNUPy0oHL8yiNC/
            KQ50PthsrD8nFHe/MojQvykOdD7YbKw/JxR3vzVh1L8ro7g+1wtfPyXvcr84Ote/LksJPtWqET8k7H6/OxPbvzECFj7USMQ/JA2QvzsT278x
            AhY+1EjEPyQNkL9Hazq/O26MPs5NpT8gtay/U8KZv0S5bT7IUog/G2a5v2AZ+L9MlR8+wldwPxSnHb9gGfi/TJUfPsJXcD8Upx2/c5gEv1pL
            9D645SA/COASv3tO0b9jpHY+ibwkPvFsVr9uYEW/ZwIvPlYlsD7N55C/bmBFv2cCLz5WJbA+zeeQv2YnMr9o8aU+LydqPrl7f79d7h6/aOcM
            PggpKD6kAVu/VbUMv2aT/z3CVcY+joi0v1W1DL9mk/89wlXGPo6ItL9Ty7W/ZffUPbA05D6I5iK/UeJev2WDlz2eFAI+gzqXv0/5CL9lNrs9
            i/MgPnsYgb9P+Qi/ZTa7PYvzID57GIG/R3eFv2OjKzztHkA+P29vv0d/Hr9mTXm86wnAPgMsNb9QCv+/bObSvYsnuD2aliC/UAr/v2zm0r2L
            J7g9mpYgv1Hw7L9uJQq9nQLQPYZJ5L9T1tq/b4b6va7d6T1k4zi/VbzHv3EMIL3AuQI9Pkpgv1W8x79xDCC9wLkCPT5KYL9d9sy/duIgvgcO
            Jjwob3i/ZjDTv3qW774tv8i87Qxlv25q2L98RX++VHFwvY0HSb9uati/fEV/vlRxcL2NB0m/e2kmv380676Ix1S+Dd0qv3PKpL97+eK+uAAU
            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
            vxgiOL8Hkeg/VmzQP0Ppfr8Kg86+5tGoP11f4T9FhVi+9+OWvr5/gD9dX+E/RYVYvvfjlr6+f4A/YLreP0YyBr7p0aq+rZeQP2OzKT9JhPa+
            204rvp2K4j9mQdk/TwUCvsxm7L6Qolw/ZkHZP08FAr7MZuy+kKJcP2tGpj9cErS+ryefvmP/kz9utLo/aSBovpBoLL4mumo/cFfOP3YuGb5h
            Iki90uqKP3BXzj92Lhm+YSJIvdLqij9wWZM/dj6dvmDdfb3ST4U/cFtWP3ZPIL5gmLK90bR/P3BdGz92X6S+YFPovdEZeD9wXRs/dl+kvmBT
            6L3RGXg/cRdWP3zKjL5EBxm9lNqnP3HRlj+AAAC+J7pNvRTatT9yi9Y/gAAAvgttfLOwAAA/covWP4AAAL4LbXyzsAAAP3KGQj+AAAC9907A
            PRTamD9yZLs/fMqMvdfaqD2U2qU/cieuP3ZfpL24hvc90RmAP3Inrj92X6S9uIb3PdEZgD9xow0/aUFuvWjxFT4mH2g/cJyJP1wjNrzEDEg+
            Y7IQP28Ykz9PBQI8DCQyPpCiXD9vGJM/TwUCPAwkMj6Qolw/blsIP0mE9jzIFSg+nYrkP21/jT9GMgY9JJqPPq2XlD9shqQ/RYVWPWSlLz6+
            f4A/bIakP0WFVj1kpS8+vn+AP2qRFD9D6Xo9sth+PubRrD9oJAg/Qk2gPfJPqz8Hkeo/ZUO+P0CxxD4YM4w/G7sAP2VDvj9AscQ+GDOMPxu7
            AD9f1J4/Px7KPlLbmD8vdNI/WMoUPy900j6Fh+w/Px7KP1BAoj8buwA+n9+OP0CxxD9QQKI/G7sAPp/fjj9AscQ/SCz4Pw8euz64y7s/QbNh
            Pz7CkD8CgnY+0CSqP0K0/j80GZA+68xiPuV2rD9Dtp4/NBmQPuvMYj7ldqw/Q7aePy1GjT7cssA+8xyuP0RQ3D8mc4w+zZkgPwBhWT9E6xo/
            H6CKPr5/gD8HNFs/RYVYPx+gij6+f4A/BzRbP0WFWD8XfHw+rZeQPw9YaD9GMgY/D1hvPp2K5D8XfHY/SYT2Pwc0Yz6Qolw/H6CDP08FAj8H
            NGM+kKJcPx+ggz9PBQI/AGFePoUa2T8mc4c/U+6iPvMctT5zJq0/LUaMP1jYQz7ldqw+XBelPzQZkD9dweQ+5XasPlwXpT80GZA/XcHkPtAk
            qD41lAA/PsKSP2X2ej64y7k+DxBaP0gs/D9uKxA+n9+MPdEZcD9QQKU/dl+kPp/fjD3RGXA/UEClP3ZfpD6FiAA9MTeAP1jKED+Bmro+Utvx
            vTE3sD9f1JU/gZq6Phg0F73RGYA/ZUOyP3ZfpD4YNBe90RmAP2VDsj92X6Q98lCkviYfaD9oJAA/aUFuPbLZWb5jshQ/apEOP1wjNj1kpqG+
            kKJgP2yGnz9PBQI9ZKahvpCiYD9shp8/TwUCPSSb4L6diuQ/bX+IP0mE9jzIF4i+rZeUP25bBj9GMgY8DChovr5/gj9vGI4/RYVWPAwoaL6+
            f4I/bxiOP0WFVrzECra+5tGsP3Cchj9D6Xq9aPCQvweR6j9xows/Qk2gvbiG2r8buwE/cietP0CxxL24htq/G7sBP3InrT9AscS919qavyWX
            7D9yZLo/P+hHvfdOxb8ucN8/coZCPzuZCL4LbYi/NQT3P3KL1j81BO++C22IvzUE9z9yi9Y/NQTvvie6Zb87mQw/cdGWPy5w2L5EBza/P+hH
            P3EXWT8ll+i+YFQNv0CxxT9wXRs/G7sCvmBUDb9AscU/cF0bPxu7Ar5gmNG/QLPMP3BbVz8boZi+YN2Vv0C10z9wWZM/G4gtvmEiWL9At9s/
            cFfOPxtuxb5hIli/QLfbP3BXzj8bbsW+kGgkv0JRrz9utLw/B18Wvq8nh79D64U/a0arPuae077MZse/RYVaP2ZB4T6+f3i+zGbHv0WFWj9m
            QeE+vn94vttOAr9GMgk/Y7MyPq2XjL7p0YG/SYT5P2C66D6diuC+9+Nrv08FBD9dX+0+kKJYvvfja79PBQQ/XV/tPpCiWL8Kg7y/XCM5P1Zs
            3D5jsgi/GCIpv2lBbz9N0nU+Jh9gvySPjL92X6Q/Q84pPdEZcL8kj4y/dl+kP0POKT3RGXC/MA8Wv4Gauj86iZA9MTeAvzqJfb+Bmro/MA8p
            vTE3wL9DzhW/dl+jPySPn73RGYi/Q84Vv3Zfoz8kj5+90RmIv03SYr9pQW0/GCJAviYfaL9WbMy/XCM3PwqD1b5jshS/XV/dv08FAj7346W+
            kKJev11f3b9PBQI+9+OlvpCiXr9guty/SYT2PunRrr6diuK/Y7Mqv0YyBz7bTiK+rZeQv2ZB3L9FhVY+zGbWvr5/fr9mQdy/RYVWPsxm1r6+
            f36/a0anv0Prgz6vJ5G+5p7Tv260uL9CUa4+kGgqvwdfFL9wV8y/QLfYPmEiWL8bbr6/cFfMv0C32D5hIli/G26+v3BZkb9AtdE+YN1wvxuI
            K79wW1e/QLPJPmCYib8boZi/cF0cv0Cxwj5gU6O/G7sEv3BdHL9AscI+YFOjvxu7BL9xF1u/P+hFPkQG6r8ll+q/cdGXvzuZCj4nujG/LnDa
            v3KL1b81BPU+C22AvzUE8b9yi9W/NQT1PgttgL81BPG/coCvvy5w3T3Xz2G/O5kLv3IF6L8ll+o9mSQgvz/oSL9xHvG/G7sAPTY2o79Asca/
            cR7xvxu7AD02NqO/QLHGv28teL8Hkei8qgucv0JNor9rRhm+5tGkva0vbL9D6X6/ZYtHvr5/dr4VKuC/RYVYv2WLR76+f3a+FSrgv0WFWL9f
            ySq+rZeIvlQNsr9GMgi/WC71vp2K2r6IEy6/SYT5v07for6QolS+pBLav08FBb9O36K+kKJUvqQS2r9PBQW/RxwnvoirFL67bAq/Umnwvz4o
            tL6As9O+0Vhev1XO2780GZC+cXkkvuV2pr9ZM8e/NBmQvnF5JL7ldqa/WTPHvx5Per5IoMK/CIVov2Hnf78IhWS+H8hgvx5Pfr9qmza+5Xag
            ve3f/L80GZO/c07tvuV2oL3t3/y/NBmTv3NO7b7go5695Eh6vzaDFb90VH6+27YIvdqw+b843Ea/dVoRvtavOr3RGXi/OyThv3Zfo77Wrzq9
            0Rl4vzsk4b92X6O+qFZrvTE3oL9QMxe/gZq6vmM5nT0xN6C/X766v4Gaur3ac5I90RmAv2jshb92X6S92nOSPdEZgL9o7IW/dl+kvTUIgD4m
            H2i/beZYv2lBbTypxaw+Y7IYv3EBMr9cIzc9sdADPpCiZL9yGiq/TwUBPbHQAz6QomS/choqv08FAT3TW+U+lxakv3Jfyr9MRPw99Q2pPp5T
            7r9yhdq/ShA+PgttkD6mESW/covUv0h16j4LbZA+phElv3KL1L9Ideo+J/8oPq1Wrr9xz9G/RvRhPkSQwD61DSa/cRPOv0X7Kj5hIlg+vPfp
            v3BXzL9Fltc/dl+kP0L4rL3RGXg+FuWKP3claj9DNya9ydjaPhlGMD9330g/Q3cqvcJjnz4bpQQ/eI1AP0O4tr26wB4+HgHwP3iNQD9DuLa9
            usAePh4B8D9/HZ4/RpDsvWHvNj44Qpg/gKurP0lpKLwyix4+UoM4P386aD9MQWA8+5uuPmzD4D9/Omg/TEFgPPubrj5sw+A/fezuP04coj1m
            4PA+feX3P3r5WD9Prb09penEPoeFjj92X6Q/UPgvPdEZgD6QEqY/dl+kP1D4Lz3RGYA+kBKmP2lBbj9VH+Y+Jh9oPquZZD9cIzY/VmtzPmOy
            ED7G518/TwUCP1VPeD6Qolw+4RL0P08FAj9VT3g+kKJcPuES9D9JhPY/VMBIPp2K5D7uRPY/RjIGP1OUmz6tl5Q++y0iP0WFVj9R22U+vn+A
            PwPWxD9FhVY/UdtlPr5/gD8D1sQ/Q+l6P05TYz7m0aw/EKVyP0JNoD9IeTI/B5HqPxyaSj9AscQ/QM1rPxu7AD8nNH8/QLHEP0DNaz8buwA/
            JzR/Pz8eyj8ysrI/L3TSPzqy/z8vdNI/HnH6Pz8eyj9JnaM/G7sAPwcqlj9AscQ/UNPFPxu7AD8HKpY/QLHEP1DTxT8OcQ8+39AOP0HBPj9Y
            CGo/AScePqtAHD9C0Lg/V4vCPue6WT5twCg/Q+AyP0w/Hj7nulk+bcAgP0PgMj9MPx4+4NBOPlM08D9EJtI/SWIgPtnmQj44qcA/RG1yP0aF
            Ij7S/Dc+Hh6YP0S0Ez9DqCI+0vw3Ph4emD9EtBM/Q6giPswn+j4G214/RPnUP0Elvz7FU7w93cC4P0U/lj8/N8c+vn+APa0FFD9FhVg/Pd45
            Pr5/gD2tBRQ/RYVYPz3eOT6tl5A9Qw36P0YyBj87xuw+nYrkPCF1Nj9JhPY/OxPDPpCiXLziMNY/TwUCPzvErj6Qoly84jDWP08FAj87xK4+
            Y7IQvYwWKT9cIzY/PIVLPiYfYL3e5XM/aUFuPz7r+z3RGXC+FuVoP3ZfpD9C+Kg90RlwvhblaD92X6Q/QvioPcnY4L4ZRhY/dyVpP0M3JT3C
            Y7a+G6TxP3ffRj9Ddyk9usBEvh4B5D94jTs/Q7i2PbrARL4eAeQ/eI07P0O4tj1h75e+OEKQP38dmT9GkO48Moz4vlKDOD+Aq6o/SWkqvPua
            qL5sw+A/fzprP0xBZLz7mqi+bMPgP386az9MQWS9ZuCdvn3l9z997PI/ThynvaXps76HhY4/evlaP0+twb3RGYC+kBKmP3ZfpD9Q+DO90RmA
            vpASpj92X6Q/UPgzviYfaL6rmWQ/aUFuP1Uf675jshS+xudgP1wjNj9Wa3i+kKJgvuES9j9PBQI/VU99vpCiYL7hEvY/TwUCP1VPfb6diuS+
            7kT3P0mE9j9UwEy+rZeUvvstJD9GMgY/U5Sgvr5/gr8D1sU/RYVWP1Hbab6+f4K/A9bFP0WFVj9R22m+5tGsvxClcj9D6Xo/TlNmvweR6r8c
            mkw/Qk2gP0h5Nr8buwG/JzSBP0CxxD9AzW6/G7sBvyc0gT9AscQ/QM1uvy900r86swY/Px7KPzKysr8/Hsm/SZ2sPy900j8ecfO/QLHFv1DT
            zT8buwI/ByqGv0Cxxb9Q080/G7sCPwcqhr9BwT6/WAhqPw5xFD7fz/a/QtC4v1eLwD8BJyg+q0ARv0PgM79MPx4+57p3Pm3AML9D4DO/TD8e
            Pue6dz5twDC/RCbUv0liHz7g0GY+UzT4v0Rtdb9GhSA+2eZTPjipwL9EtBW/Q6ggPtL8QT4eHpC/RLQVv0OoID7S/EE+Hh6Qv0T5179BJbw+
            zCf+PgbbRr9FP5i/PzfFPsVTuz3dwGi/RYVavz3eNz6+f3g9rQSiv0WFWr893jc+vn94Pa0Eor9GMgm/O8bsPq2XjD1DDWC/SYT5vzsTwz6d
            iuA8IXQav08FBL87xKw+kKJYvOIwvL9PBQS/O8SsPpCiWLziMLy/XCM5vzyFST5jsgi9jBZIv2lBb78+6/w+Jh9gvd7ltL92X6S/QvitPdEZ
            cL4W5Ze/dl+kv0L4rT3RGXC+FuWXv3clar9DNyc9ydjSvhlGMr9330i/Q3cqPcJjlr4bpPu/eI1Av0O4tD26wBS+HgHcv3iNQL9DuLQ9usAU
            vh4B3L9/HaC/RpDtPWHu3744Qoy/gKusv0lpJzwyiEy+UoM0v386Zb9MQWC8+52OvmzD4L9/OmW/TEFgvPudjr5sw+C/fezqv04cor1m4ZG+
            feX3v3r5VL9Prby9pensvoeFjr92X6O/UPguvdEZiL6QEqa/dl+jv1D4Lr3RGYi+kBKmv2lBbb9VH+O+Jh9ovquZWb9cIze/VmtyvmOyFL7G
            50q/TwUCv1VPeL6Qol6+4RLUv08FAr9VT3i+kKJevuES1L9JhPa/VMBJvp2K4r7uRNe/RjIHv1OUnb6tl5C++y0Fv0WFVr9R22e+vn9+vwPW
            tr9FhVa/Udtnvr5/fr8D1ra/Q+l7v05TYr7m0ay/EKV1v0JNn79IeSe/B5HtvxyaXb9AscK/QM1Tvxu7BL8nNJ+/QLHCv0DNU78buwS/JzSf
            vz8ex78yspe/L3TUvzqzFL8vdNC/HnHgvz8ey79Jna6/G7sAvwcqgb9Asca/UNPLvxu7AL8HKoG/QLHGv1DTy78OcQ2+38/rv0HBQL9YCGq/
            ASccvqtABb9C0Lu/V4vAvue6U75twBi/Q+A0v0w/Hr7nulO+bcAYv0PgNL9MPx6+4NBMvlM06L9EJtS/SWIevtnmRL44qbi/RG10v0aFIL7S
            /Dy+Hh6Iv0S0Fb9DqCC+0vw8vh4eiL9EtBW/Q6ggvswn+r4G2z6/RPnVv0ElvL7FU7i93cBYv0U/lr8/N8W+vn92va0Ek79FhVi/Pd43vr5/
            dr2tBJO/RYVYvz3eN76tl4i9Qw0Wv0YyCL87xuu+nYravCFyQr9JhPm/OxPCvpCiVDziMgK/TwUFvzvErr6QolQ84jICv08FBb87xK6+Y7IE
            PYwWb79cIzm/PIVKviYfYD3e5bO/aUFvvz7r+73RGXg+FuWEv3Zfo79C+Km90Rl4PhblhL92X6O/QvipvcnY+j4ZRiq/dyVmv0M3I73CY+I+
            G6T9v3ffQ79Ddye9usCEPh4B6L94jTe/Q7izvbrAhD4eAei/eI03v0O4s71h7/g+OEKYv38dmr9GkO28Mo1SPlKDQL+Aq6y/SWkmPPua/D5s
            w/i/fzpqv0xBYDz7mvw+bMP4v386ar9MQWA9ZuC3Pn3mD7997PG/ThyiPaXpuD6HhZq/evlav0+tvD3RGYA+kBKxv3ZfpL9Q+C490RmAPpAS
            sb92X6S/UPguPiYfaD6rmW6/aUFtv1Uf5D5jshg+xudqv1wjN79Wa3I+kKJkPuETAL9PBQG/VU92PpCiZD7hEwC/TwUBv1VPdj6diug+7kUB
            v0mE9r9UwEU+rZeUPvstLb9GMgW/U5SYPr5/gD8D1sm/RYVWv1HbYT6+f4A/A9bJv0WFVr9R22E+5tGwPxCldr9D6Xi/TlNePweR7j8cmk6/
            Qk2dv0h5LD8buwQ/JzSDv0Cxwr9AzWQ/G7sEPyc0g79AscK/QM1kPy901j86swK/Px7HvzKyqz8/Hs4/SZ2lvy90zr8ecfM/QLHIP1DTx78b
            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
            """;

    private LoadingIndicatorShapes() {

    }

    static float[][] decodeMorphs() {
        final FloatBuffer floats = ByteBuffer.wrap(Base64.decode(MORPHS, Base64.DEFAULT)).asFloatBuffer();
        final float[][] morphs = new float[DETERMINATE_MORPH_INDEX + 1][];
        for (int i = 0; i < morphs.length; i++) {
            final int cubics = (i == DETERMINATE_MORPH_INDEX) ? DETERMINATE_CUBIC_COUNT : INDETERMINATE_CUBIC_COUNTS[i];
            morphs[i] = new float[cubics * FLOATS_PER_CUBIC];
            floats.get(morphs[i]);
        }
        return morphs;
    }

}
