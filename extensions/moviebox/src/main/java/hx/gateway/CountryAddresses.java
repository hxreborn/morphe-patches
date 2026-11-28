/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package hx.gateway;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

final class CountryAddresses {

    private static final int FIRST_HOST = 1;

    private static final int HOSTS_PER_NETWORK = 254;

    private static final Map<String, String> NETWORKS = new HashMap<>();

    private static final int HOST = FIRST_HOST + new Random().nextInt(HOSTS_PER_NETWORK);

    static {
        NETWORKS.put("AD", "46.172.240");
        NETWORKS.put("AE", "94.204.0");
        NETWORKS.put("AF", "149.54.64");
        NETWORKS.put("AG", "69.50.72");
        NETWORKS.put("AI", "208.66.52");
        NETWORKS.put("AL", "79.106.128");
        NETWORKS.put("AM", "46.71.128");
        NETWORKS.put("AO", "105.172.0");
        NETWORKS.put("AR", "186.136.0");
        NETWORKS.put("AS", "103.117.169");
        NETWORKS.put("AT", "77.118.0");
        NETWORKS.put("AU", "1.144.0");
        NETWORKS.put("AW", "201.229.64");
        NETWORKS.put("AZ", "5.191.128");
        NETWORKS.put("BA", "31.176.192");
        NETWORKS.put("BB", "196.3.208");
        NETWORKS.put("BD", "114.130.128");
        NETWORKS.put("BE", "109.136.0");
        NETWORKS.put("BF", "102.179.0");
        NETWORKS.put("BG", "95.43.0");
        NETWORKS.put("BH", "15.185.0");
        NETWORKS.put("BI", "154.117.224");
        NETWORKS.put("BJ", "137.255.128");
        NETWORKS.put("BM", "196.12.96");
        NETWORKS.put("BN", "156.31.128");
        NETWORKS.put("BO", "179.59.0");
        NETWORKS.put("BR", "179.144.0");
        NETWORKS.put("BS", "24.51.96");
        NETWORKS.put("BT", "119.2.112");
        NETWORKS.put("BW", "168.167.128");
        NETWORKS.put("BY", "178.124.0");
        NETWORKS.put("BZ", "190.197.64");
        NETWORKS.put("CA", "9.96.0");
        NETWORKS.put("CD", "41.243.128");
        NETWORKS.put("CF", "197.242.180");
        NETWORKS.put("CG", "160.113.128");
        NETWORKS.put("CH", "85.4.0");
        NETWORKS.put("CI", "102.138.0");
        NETWORKS.put("CK", "202.65.56");
        NETWORKS.put("CL", "191.116.0");
        NETWORKS.put("CM", "102.246.0");
        NETWORKS.put("CN", "36.160.0");
        NETWORKS.put("CO", "181.144.0");
        NETWORKS.put("CR", "201.200.0");
        NETWORKS.put("CU", "169.158.128");
        NETWORKS.put("CV", "165.90.112");
        NETWORKS.put("CY", "37.149.128");
        NETWORKS.put("CZ", "85.162.0");
        NETWORKS.put("DE", "53.128.0");
        NETWORKS.put("DJ", "197.241.64");
        NETWORKS.put("DK", "80.164.0");
        NETWORKS.put("DM", "66.118.38");
        NETWORKS.put("DO", "152.167.0");
        NETWORKS.put("DZ", "105.104.0");
        NETWORKS.put("EC", "181.113.0");
        NETWORKS.put("EE", "90.191.0");
        NETWORKS.put("EG", "156.176.0");
        NETWORKS.put("ER", "196.200.104");
        NETWORKS.put("ES", "83.48.0");
        NETWORKS.put("ET", "196.191.0");
        NETWORKS.put("FI", "91.156.0");
        NETWORKS.put("FJ", "144.120.128");
        NETWORKS.put("FK", "80.73.212");
        NETWORKS.put("FM", "119.252.120");
        NETWORKS.put("FO", "88.85.48");
        NETWORKS.put("FR", "86.224.0");
        NETWORKS.put("GA", "41.159.0");
        NETWORKS.put("GB", "25.128.0");
        NETWORKS.put("GD", "74.122.92");
        NETWORKS.put("GE", "31.146.128");
        NETWORKS.put("GH", "154.168.0");
        NETWORKS.put("GI", "85.115.144");
        NETWORKS.put("GL", "88.83.16");
        NETWORKS.put("GM", "160.183.0");
        NETWORKS.put("GN", "197.149.224");
        NETWORKS.put("GQ", "105.235.232");
        NETWORKS.put("GR", "94.68.0");
        NETWORKS.put("GT", "168.234.128");
        NETWORKS.put("GW", "197.214.88");
        NETWORKS.put("GY", "190.80.64");
        NETWORKS.put("HK", "113.254.0");
        NETWORKS.put("HN", "181.210.128");
        NETWORKS.put("HR", "93.140.0");
        NETWORKS.put("HT", "148.102.192");
        NETWORKS.put("HU", "89.134.0");
        NETWORKS.put("ID", "39.224.0");
        NETWORKS.put("IE", "34.248.0");
        NETWORKS.put("IN", "117.224.0");
        NETWORKS.put("IQ", "37.238.0");
        NETWORKS.put("IR", "2.184.0");
        NETWORKS.put("IS", "130.208.128");
        NETWORKS.put("IT", "79.16.0");
        NETWORKS.put("JM", "72.27.64");
        NETWORKS.put("JO", "176.29.128");
        NETWORKS.put("JP", "133.64.0");
        NETWORKS.put("KE", "105.56.0");
        NETWORKS.put("KG", "158.181.192");
        NETWORKS.put("KH", "36.37.192");
        NETWORKS.put("KI", "103.250.2");
        NETWORKS.put("KM", "197.255.232");
        NETWORKS.put("KN", "45.42.254");
        NETWORKS.put("KP", "175.45.178");
        NETWORKS.put("KR", "121.160.0");
        NETWORKS.put("KW", "37.38.0");
        NETWORKS.put("KY", "148.51.64");
        NETWORKS.put("KZ", "2.76.0");
        NETWORKS.put("LA", "115.84.96");
        NETWORKS.put("LB", "178.135.128");
        NETWORKS.put("LC", "24.92.152");
        NETWORKS.put("LI", "82.117.16");
        NETWORKS.put("LK", "112.135.0");
        NETWORKS.put("LR", "105.176.128");
        NETWORKS.put("LS", "129.232.64");
        NETWORKS.put("LT", "78.60.0");
        NETWORKS.put("LU", "158.167.0");
        NETWORKS.put("LV", "5.241.0");
        NETWORKS.put("LY", "41.254.0");
        NETWORKS.put("MA", "105.144.0");
        NETWORKS.put("MC", "88.209.96");
        NETWORKS.put("MD", "92.115.128");
        NETWORKS.put("ME", "31.204.224");
        NETWORKS.put("MG", "102.18.0");
        NETWORKS.put("MK", "77.29.0");
        NETWORKS.put("ML", "154.118.160");
        NETWORKS.put("MM", "37.111.64");
        NETWORKS.put("MN", "49.0.144");
        NETWORKS.put("MO", "60.246.128");
        NETWORKS.put("MQ", "95.138.64");
        NETWORKS.put("MR", "41.138.144");
        NETWORKS.put("MS", "82.99.104");
        NETWORKS.put("MT", "46.11.128");
        NETWORKS.put("MU", "102.232.0");
        NETWORKS.put("MV", "27.114.160");
        NETWORKS.put("MW", "102.71.0");
        NETWORKS.put("MX", "189.160.0");
        NETWORKS.put("MY", "175.140.0");
        NETWORKS.put("MZ", "197.219.0");
        NETWORKS.put("NA", "41.182.128");
        NETWORKS.put("NC", "101.101.32");
        NETWORKS.put("NE", "197.214.32");
        NETWORKS.put("NG", "105.120.0");
        NETWORKS.put("NI", "186.77.0");
        NETWORKS.put("NL", "145.112.0");
        NETWORKS.put("NO", "84.212.0");
        NETWORKS.put("NP", "36.253.0");
        NETWORKS.put("NU", "49.156.50");
        NETWORKS.put("NZ", "122.60.0");
        NETWORKS.put("OM", "5.37.0");
        NETWORKS.put("PA", "186.74.0");
        NETWORKS.put("PE", "190.236.0");
        NETWORKS.put("PF", "123.50.96");
        NETWORKS.put("PG", "124.240.208");
        NETWORKS.put("PH", "49.148.0");
        NETWORKS.put("PK", "39.48.0");
        NETWORKS.put("PL", "83.16.0");
        NETWORKS.put("PM", "70.36.8");
        NETWORKS.put("PR", "66.50.128");
        NETWORKS.put("PS", "188.161.128");
        NETWORKS.put("PT", "85.244.0");
        NETWORKS.put("PW", "57.70.248");
        NETWORKS.put("PY", "181.124.0");
        NETWORKS.put("QA", "4.171.0");
        NETWORKS.put("RE", "102.35.128");
        NETWORKS.put("RO", "92.84.0");
        NETWORKS.put("RS", "93.87.0");
        NETWORKS.put("RU", "5.140.0");
        NETWORKS.put("RW", "41.186.128");
        NETWORKS.put("SA", "188.52.0");
        NETWORKS.put("SB", "202.1.176");
        NETWORKS.put("SC", "137.63.64");
        NETWORKS.put("SD", "102.124.0");
        NETWORKS.put("SE", "78.72.0");
        NETWORKS.put("SG", "13.214.0");
        NETWORKS.put("SI", "188.198.0");
        NETWORKS.put("SK", "78.99.0");
        NETWORKS.put("SL", "102.143.64");
        NETWORKS.put("SM", "89.186.48");
        NETWORKS.put("SN", "41.83.0");
        NETWORKS.put("SO", "154.115.224");
        NETWORKS.put("SR", "186.179.192");
        NETWORKS.put("SS", "105.235.212");
        NETWORKS.put("ST", "197.159.176");
        NETWORKS.put("SV", "179.5.128");
        NETWORKS.put("SY", "5.0.128");
        NETWORKS.put("SZ", "41.84.240");
        NETWORKS.put("TD", "154.68.144");
        NETWORKS.put("TG", "196.170.0");
        NETWORKS.put("TH", "171.100.0");
        NETWORKS.put("TK", "27.96.28");
        NETWORKS.put("TM", "95.85.112");
        NETWORKS.put("TN", "197.16.0");
        NETWORKS.put("TO", "175.176.148");
        NETWORKS.put("TR", "78.168.0");
        NETWORKS.put("TT", "186.45.0");
        NETWORKS.put("TV", "202.2.112");
        NETWORKS.put("TW", "120.112.0");
        NETWORKS.put("TZ", "156.158.0");
        NETWORKS.put("UA", "37.54.0");
        NETWORKS.put("UG", "102.84.0");
        NETWORKS.put("US", "29.0.0");
        NETWORKS.put("UY", "167.60.0");
        NETWORKS.put("UZ", "84.54.96");
        NETWORKS.put("VC", "207.191.244");
        NETWORKS.put("VE", "186.92.0");
        NETWORKS.put("VG", "67.209.168");
        NETWORKS.put("VI", "146.226.128");
        NETWORKS.put("VN", "14.176.0");
        NETWORKS.put("VU", "202.77.72");
        NETWORKS.put("WS", "202.4.48");
        NETWORKS.put("YE", "134.35.128");
        NETWORKS.put("ZA", "41.16.0");
        NETWORKS.put("ZM", "102.148.0");
        NETWORKS.put("ZW", "41.60.96");
    }

    private CountryAddresses() {
    }

    static String addressIn(String isoCode) {
        String network = NETWORKS.get(isoCode);
        return (network != null) ? network + "." + HOST : null;
    }

}
