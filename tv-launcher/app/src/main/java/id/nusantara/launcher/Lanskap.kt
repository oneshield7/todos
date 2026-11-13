package id.nusantara.launcher

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Path
import androidx.core.graphics.PathParser

/** Satu bentuk gambar: jalur, warna, dan tebal garis (0 berarti bentuk terisi). */
class Bentuk(val jalur: Path, val warna: Int, val garis: Float = 0f)

/**
 * Gambar lanskap tiap tema pada bidang 1920 x 1080, dalam warna siang hari.
 * Waktu dan cuaca diberi warna belakangan oleh LatarView.
 */
object Lanskap {
    private val simpanan = HashMap<Tema, List<Bentuk>>()

    fun bentuk(tema: Tema): List<Bentuk> = simpanan.getOrPut(tema) {
        when (tema) {
            Tema.GEDUNG_SATE -> gedungSate()
            Tema.MONAS -> kota(true)
            Tema.PRAMBANAN -> prambanan()
            Tema.MERU -> meruBali()
            Tema.AMPERA -> ampera()
            Tema.PASAR_TERAPUNG -> pasarTerapung()
            Tema.PINISI -> pinisi()
            Tema.HONAI -> honai()
            Tema.PEGUNUNGAN -> pegunungan()
            Tema.PESISIR -> pesisir()
            Tema.KOTA -> kota(false)
        }
    }

    // ---- alat bantu ----

    private fun isi(d: String, warna: String) = Bentuk(PathParser.createPathFromPathData(d), Color.parseColor(warna))

    private fun garis(d: String, warna: String, tebal: Float) =
        Bentuk(PathParser.createPathFromPathData(d), Color.parseColor(warna), tebal)

    private fun kotak(x: Float, y: Float, w: Float, h: Float) = "M$x $y h$w v$h h${-w} Z"

    private fun elips(cx: Float, cy: Float, rx: Float, ry: Float) =
        "M${cx - rx} $cy a$rx $ry 0 1 0 ${rx * 2f} 0 a$rx $ry 0 1 0 ${-rx * 2f} 0 Z"

    private fun poli(titik: String) = "M" + titik.trim().split(" ").joinToString(" L") + " Z"

    private fun pindah(b: Bentuk, tx: Float, ty: Float, skala: Float): Bentuk {
        val jalur = Path(b.jalur)
        val m = Matrix()
        m.setScale(skala, skala)
        m.postTranslate(tx, ty)
        jalur.transform(m)
        return Bentuk(jalur, b.warna, b.garis * skala)
    }

    // ---- Jawa Barat: Gedung Sate, Tangkuban Perahu, kebun teh ----

    private fun gedungSate(): List<Bentuk> = buildList {
        add(isi("M0 580 L170 486 L300 530 L520 410 L640 474 L770 432 Q840 396 910 400 L1250 394 Q1330 394 1390 432 L1560 520 L1700 474 L1920 548 V760 H0 Z", "#6F8E8C"))
        add(isi("M0 650 Q240 560 520 612 T1000 604 T1500 612 T1920 584 V800 H0 Z", "#4B6E62"))
        add(isi("M1092 700 V580 H1080 L1112 552 H1286 V520 H1272 L1308 486 H1338 V446 H1318 L1346 418 H1356 V398 H1340 L1362 376 H1368 V360 H1358 L1380 336 L1402 360 H1392 V376 H1398 L1420 398 H1404 V418 H1414 L1442 446 H1422 V486 H1452 L1488 520 H1474 V552 H1648 L1680 580 H1668 V700 Z", "#26363B"))
        add(isi(kotak(1378.5f, 284f, 3f, 54f), "#26363B"))
        for (y in 288..328 step 8) add(isi(elips(1380f, y.toFloat(), 4.2f, 4.2f), "#26363B"))
        for (i in 0 until 21) add(isi(kotak(1112f + 26f * i, 602f, 9f, 24f), "#F4CC7A"))
        for (i in 0 until 5) add(isi(kotak(1320f + 26f * i, 558f, 9f, 24f), "#F4CC7A"))
        add(isi("M0 716 Q480 610 960 668 T1920 650 V1080 H0 Z", "#3F7A4E"))
        add(garis("M0 752 Q480 646 960 704 T1920 686", "#5E9C6A", 3f))
        add(garis("M0 790 Q480 684 960 742 T1920 724", "#5E9C6A", 3f))
        add(isi("M0 838 Q500 742 1000 800 T1920 776 V1080 H0 Z", "#2F6140"))
    }

    // ---- Bali: meru, candi bentar, Gunung Agung, sawah ----

    private fun meru(cx: Float, tingkat: Int, setengahBawah: Float, setengahAtas: Float): List<Bentuk> {
        val atas = 530f - 22f * tingkat + 8f
        fun setengah(y: Float) = setengahAtas + (setengahBawah - setengahAtas) * (y - atas) / (530f - atas)
        return buildList {
            add(isi(kotak(cx - 7f, atas, 14f, 530f - atas), "#2B211C"))
            add(isi(poli("${cx - 8f},$atas $cx,${atas - 24f} ${cx + 8f},$atas"), "#2B211C"))
            for (i in 0 until tingkat) {
                val bawah = 530f - 22f * i
                val puncak = bawah - 14f
                add(isi(poli("${cx - setengah(bawah)},$bawah ${cx + setengah(bawah)},$bawah ${cx + setengah(puncak)},$puncak ${cx - setengah(puncak)},$puncak"), "#2B211C"))
            }
        }
    }

    private fun meruBali(): List<Bentuk> = buildList {
        add(isi("M640 640 L1020 330 Q1060 300 1100 330 L1500 640 Z", "#7FB4CC"))
        add(isi("M1380 640 L1610 486 Q1640 470 1670 486 L1920 620 V640 Z", "#97C4D6"))
        add(isi("M1074 600 V410 H1088 V432 H1102 V456 H1118 V486 H1136 V522 H1156 V600 Z", "#9A5234"))
        add(isi("M1046 600 V410 H1032 V432 H1018 V456 H1002 V486 H984 V522 H964 V600 Z", "#9A5234"))
        add(isi(kotak(1170f, 566f, 450f, 40f), "#9A5234"))
        add(isi(kotak(1164f, 560f, 462f, 8f), "#5E3221"))
        add(isi(kotak(1232f, 530f, 76f, 40f), "#7A5A48"))
        add(isi(kotak(1354f, 530f, 92f, 40f), "#7A5A48"))
        add(isi(kotak(1486f, 530f, 68f, 40f), "#7A5A48"))
        addAll(meru(1270f, 7, 64f, 18f))
        addAll(meru(1400f, 11, 78f, 20f))
        addAll(meru(1520f, 5, 54f, 16f))
        add(garis("M1736 612 Q1746 520 1728 440", "#4A3828", 8f))
        add(garis("M1810 612 Q1804 540 1818 484", "#4A3828", 7f))
        add(garis("M1728 440 Q1684 412 1648 446 M1728 440 Q1692 396 1656 400 M1728 440 Q1728 392 1706 372 M1728 440 Q1760 398 1796 402 M1728 440 Q1772 414 1806 450", "#1F6B3C", 7f))
        add(garis("M1818 484 Q1786 462 1758 486 M1818 484 Q1792 448 1768 450 M1818 484 Q1820 448 1806 432 M1818 484 Q1844 452 1872 458 M1818 484 Q1852 466 1876 494", "#1F6B3C", 7f))
        add(isi("M0 590 Q420 560 900 606 T1920 596 V1080 H0 Z", "#4E9E55"))
        add(garis("M0 597 Q420 567 900 613 T1920 603", "#CFF0D0", 3f))
        add(isi("M0 660 Q500 612 1000 664 T1920 648 V1080 H0 Z", "#3A8749"))
        add(garis("M0 667 Q500 619 1000 671 T1920 655", "#BFE6C4", 3f))
        add(isi("M0 740 Q520 690 1040 742 T1920 724 V1080 H0 Z", "#2B6E3E"))
        add(isi("M0 830 Q540 784 1080 832 T1920 812 V1080 H0 Z", "#1F5632"))
    }

    // ---- Sumatera Selatan: Jembatan Ampera di atas Sungai Musi ----

    private fun ampera(): List<Bentuk> = buildList {
        add(isi("M0 640 V612 H140 V596 H260 V606 H420 V590 H520 V604 H700 V598 H860 V610 H1040 V600 H1920 V640 Z", "#5E7078"))
        for (x in listOf(640f, 820f, 1000f, 1660f, 1840f)) add(isi(kotak(x, 566f, 18f, 74f), "#4A5560"))
        add(isi(kotak(1168f, 566f, 44f, 80f), "#4A5560"))
        add(isi(kotak(1468f, 566f, 44f, 80f), "#4A5560"))
        add(isi(kotak(560f, 552f, 1360f, 14f), "#8E2620"))
        for (i in 0 until 48) add(isi(elips(564f + 28f * i, 546f, 2.2f, 2.2f), "#FFE2A0"))
        for (x in listOf(1168f, 1468f)) {
            add(isi(kotak(x, 320f, 12f, 236f), "#C23A30"))
            add(isi(kotak(x + 32f, 320f, 12f, 236f), "#C23A30"))
            add(isi(kotak(x, 388f, 44f, 8f), "#C23A30"))
            add(isi(kotak(x, 456f, 44f, 8f), "#C23A30"))
            add(isi(kotak(x - 8f, 300f, 60f, 24f), "#C23A30"))
            add(isi(kotak(x + 4f, 290f, 36f, 10f), "#C23A30"))
        }
        add(isi("M0 640 H1920 V1080 H0 Z", "#4F7F98"))
        add(isi(kotak(1170f, 652f, 40f, 130f), "#7A5560"))
        add(isi(kotak(1470f, 652f, 40f, 130f), "#7A5560"))
        add(garis("M700 668 h120 M980 700 h180 M1300 684 h90 M1560 720 h160 M840 760 h140 M1380 790 h200", "#9CC3D6", 3f))
        add(isi("M860 724 Q930 750 1000 724 Q930 734 860 724 Z", "#2A2420"))
        add(isi(kotak(906f, 712f, 48f, 16f), "#2A2420"))
    }

    // ---- Kalimantan Selatan: pasar terapung ----

    private fun perahu(tx: Float, ty: Float, skala: Float, muatan: String, baju: String, orangDiKiri: Boolean): List<Bentuk> {
        val o = if (orangDiKiri) -108f else 0f
        val m = if (orangDiKiri) 78f else 0f
        return listOf(
            isi("M0 0 Q150 46 300 0 Q150 14 0 0 Z", "#3A2E28"),
            isi(elips(112f + m, 6f, 42f, 12f), muatan),
            isi("M${186f + o} 12 L${194f + o} -26 H${214f + o} L${222f + o} 12 Z", baju),
            isi(elips(204f + o, -34f, 8f, 8f), "#3A2E28"),
            isi(poli("${176f + o},-34 ${204f + o},-52 ${232f + o},-34"), "#D8C08A"),
        ).map { pindah(it, tx, ty, skala) }
    }

    private fun pasarTerapung(): List<Bentuk> = buildList {
        add(isi("M0 620 Q120 590 260 606 T560 600 T900 610 T1300 604 T1620 598 T1920 606 V660 H0 Z", "#6F8F7A"))
        for ((x, lebar, tinggi) in listOf(Triple(1486f, 88f, 34f), Triple(1604f, 112f, 40f), Triple(1766f, 78f, 30f))) {
            val atap = 630f - tinggi
            add(isi(poli("${x - 6f},$atap ${x + lebar / 2f},${atap - lebar * 0.34f} ${x + lebar + 6f},$atap"), "#6A5A4E"))
            add(isi(kotak(x, atap, lebar, tinggi), "#7A6A5C"))
            for (k in 0..2) add(isi(kotak(x + 6f + k * (lebar - 16f) / 2f, 630f, 4f, 24f), "#5A4C42"))
        }
        add(isi("M0 648 H1920 V1080 H0 Z", "#7FA9B4"))
        add(isi("M0 800 H1920 V1080 H0 Z", "#6B97A3"))
        add(garis("M300 690 h220 M760 670 h160 M1100 664 h180 M1500 700 h240 M1150 790 h200", "#CFE4E8", 3f))
        addAll(perahu(1320f, 690f, 0.7f, "#5E8A3A", "#4E5A7A", false))
        addAll(perahu(880f, 730f, 1f, "#D9822B", "#7A3B4A", false))
        addAll(perahu(1500f, 770f, 1.15f, "#C23B2B", "#2F6A5A", true))
    }

    // ---- Sulawesi Selatan: perahu pinisi ----

    private fun pinisi(): List<Bentuk> = buildList {
        add(isi("M0 650 H1920 V1080 H0 Z", "#2F7F9F"))
        add(isi("M180 650 Q300 622 470 650 Z", "#3F6B58"))
        add(garis("M200 720 h160 M620 760 h220 M1100 730 h140 M1500 790 h240 M360 840 h200", "#7FC0D6", 3f))
        for (layar in listOf(
            "1090,560 1336,336 1216,598", "1150,580 1336,398 1268,600", "1212,594 1336,458 1318,602",
            "1346,382 1440,352 1474,588 1346,588", "1346,372 1346,304 1434,346",
            "1506,368 1596,340 1632,590 1506,590", "1506,358 1506,292 1590,334",
        )) {
            add(isi(poli(layar), "#EDE3D0"))
            add(garis(poli(layar), "#B9AC94", 2f))
        }
        add(isi(kotak(1338f, 300f, 6f, 310f), "#2A1A22"))
        add(isi(kotak(1498f, 286f, 6f, 324f), "#2A1A22"))
        add(isi("M1196 604 L1230 640 Q1260 652 1300 652 H1560 Q1610 650 1650 598 L1610 610 H1240 Z", "#2A1A22"))
        add(garis("M1210 606 L1080 556", "#2A1A22", 5f))
    }

    // ---- Papua Pegunungan: honai di Lembah Baliem ----

    private fun honai(): List<Bentuk> = buildList {
        add(isi("M0 560 L140 420 L260 500 L420 330 L560 450 L700 380 L860 520 L1000 400 L1150 300 L1300 430 L1440 360 L1600 480 L1760 390 L1920 470 V700 H0 Z", "#6E94B4"))
        add(isi("M0 620 Q300 520 640 590 T1280 570 T1920 560 V760 H0 Z", "#3F7A5E"))
        add(isi("M0 690 Q480 650 960 684 T1920 668 V1080 H0 Z", "#86B04F"))
        add(isi(kotak(1240f, 654f, 120f, 46f), "#6B4A2E"))
        add(isi(kotak(1401f, 637f, 138f, 53f), "#6B4A2E"))
        add(isi(kotak(1580f, 666f, 100f, 38f), "#6B4A2E"))
        add(isi("M1222 656 Q1230 580 1300 564 Q1370 580 1378 656 Z", "#9A7740"))
        add(isi("M1380 640 Q1390 552 1470 534 Q1550 552 1560 640 Z", "#9A7740"))
        add(isi("M1565 668 Q1572 604 1630 590 Q1688 604 1695 668 Z", "#9A7740"))
        add(isi(kotak(1288f, 666f, 24f, 34f), "#2E1F12"))
        add(isi(kotak(1456f, 651f, 28f, 39f), "#2E1F12"))
        add(isi(kotak(1620f, 676f, 20f, 28f), "#2E1F12"))
        for (x in 1170..1770 step 45) add(isi(kotak(x.toFloat(), 690f, 5f, 36f), "#4A3420"))
        add(isi(kotak(1150f, 700f, 630f, 4f), "#4A3420"))
        add(isi(kotak(1150f, 714f, 630f, 4f), "#4A3420"))
        add(isi("M0 800 Q500 760 1000 796 T1920 776 V1080 H0 Z", "#5E8E3A"))
    }

    // ---- Bentang alam umum untuk provinsi yang ikonnya belum digambar ----

    private fun pegunungan(): List<Bentuk> = buildList {
        add(isi("M0 600 L180 430 L330 520 L560 340 L760 500 L940 400 L1150 300 L1360 460 L1540 380 L1760 500 L1920 440 V760 H0 Z", "#7C9BB0"))
        add(isi("M0 660 Q300 540 640 610 T1280 590 T1920 580 V800 H0 Z", "#54806B"))
        for (i in 0 until 14) {
            val x = 1010f + i * 62f
            val dasar = 676f - (i % 3) * 8f
            add(isi(poli("$x,$dasar ${x + 22f},${dasar - 74f} ${x + 44f},$dasar"), "#2F5A44"))
        }
        add(isi("M0 720 Q480 660 960 700 T1920 684 V1080 H0 Z", "#3E6B4F"))
        add(isi("M0 840 Q500 780 1000 820 T1920 800 V1080 H0 Z", "#2F5640"))
    }

    private fun pesisir(): List<Bentuk> = buildList {
        add(isi("M860 640 Q1080 548 1300 596 T1740 640 Z", "#5F8F86"))
        add(isi("M0 640 H1920 V1080 H0 Z", "#3E8FB0"))
        add(garis("M160 700 h200 M620 740 h260 M1040 690 h180 M360 820 h240 M900 860 h200", "#9FD3E3", 3f))
        add(isi("M1920 700 Q1500 720 1200 820 T700 1080 H1920 Z", "#E6D2A0"))
        add(garis("M1640 800 Q1660 660 1620 540", "#5A4632", 10f))
        add(garis("M1790 840 Q1784 730 1770 630", "#5A4632", 9f))
        add(garis("M1620 540 Q1560 500 1500 548 M1620 540 Q1574 480 1524 486 M1620 540 Q1622 474 1592 446 M1620 540 Q1666 482 1716 490 M1620 540 Q1684 506 1730 556", "#2E7A4A", 8f))
        add(garis("M1770 630 Q1722 598 1676 636 M1770 630 Q1734 582 1694 586 M1770 630 Q1774 578 1750 552 M1770 630 Q1808 584 1850 590 M1770 630 Q1822 604 1858 646", "#2E7A4A", 8f))
    }

    // ---- DI Yogyakarta: Candi Prambanan dengan Gunung Merapi ----

    private fun prambanan(): List<Bentuk> = buildList {
        val candi = "M1300 650 V570 H1316 V530 H1334 V480 H1350 V435 H1364 V395 H1378 V360 H1390 L1400 318 L1410 360 H1422 V395 H1436 V435 H1450 V480 H1466 V530 H1484 V570 H1500 V650 Z"
        add(isi("M520 660 L900 372 Q925 354 952 370 L1010 418 Q1080 470 1170 506 L1420 660 Z", "#7C9BB0"))
        add(garis("M926 356 Q940 310 985 292 Q1035 272 1050 226", "#DCE6EC", 16f))
        add(isi("M0 640 Q300 600 640 624 T1280 610 T1920 600 V700 H0 Z", "#54806B"))
        add(pindah(isi(candi, "#4A4A52"), 502f, 377f, 0.42f))
        add(pindah(isi(candi, "#4A4A52"), 1122f, 377f, 0.42f))
        add(pindah(isi(candi, "#4A4A52"), 207f, 182f, 0.72f))
        add(pindah(isi(candi, "#4A4A52"), 577f, 182f, 0.72f))
        add(isi(candi, "#3F3F47"))
        add(pindah(isi(candi, "#5A5A62"), 608f, 325f, 0.5f))
        add(pindah(isi(candi, "#5A5A62"), 792f, 325f, 0.5f))
        add(isi("M0 650 H1920 V1080 H0 Z", "#6E9E48"))
        add(isi(elips(760f, 652f, 120f, 22f), "#3F6B4F"))
        add(isi(elips(980f, 650f, 90f, 28f), "#3F6B4F"))
        add(isi(elips(1840f, 648f, 110f, 34f), "#3F6B4F"))
        add(isi("M0 760 Q600 700 1200 752 T1920 730 V1080 H0 Z", "#4F7F3A"))
    }

    // ---- Perkotaan; dengan Monas untuk Jakarta ----

    private fun kota(monas: Boolean): List<Bentuk> = buildList {
        add(isi("M0 620 Q400 560 800 600 T1920 580 V700 H0 Z", "#8FA3AE"))
        add(isi("M640 650 V560 H700 V520 H760 V580 H820 V470 H880 V540 H950 V500 H1010 V440 H1070 V530 H1140 V480 H1200 V560 H1260 V510 H1310 V650 Z", "#7A8794"))
        add(isi("M1540 650 V520 H1600 V450 H1660 V540 H1720 V410 H1790 V500 H1850 V460 H1920 V650 Z", "#7A8794"))
        add(isi("M560 650 V600 H640 V540 H720 V600 H800 V570 H900 V600 H980 V520 H1050 V590 H1150 V560 H1230 V610 H1290 V650 Z", "#4A5560"))
        add(isi("M1560 650 V590 H1640 V540 H1700 V600 H1780 V500 H1840 V580 H1920 V650 Z", "#4A5560"))
        for ((x0, y0, kolom, baris) in listOf(listOf(653, 556, 4, 4), listOf(995, 534, 3, 5), listOf(1661, 556, 2, 4), listOf(1787, 512, 3, 6))) {
            for (k in 0 until kolom) for (b in 0 until baris) {
                add(isi(kotak(x0 + k * 18f, y0 + b * 22f, 6f, 8f), "#FFD98A"))
            }
        }
        if (monas) {
            add(isi(poli("1335,650 1345,622 1505,622 1515,650"), "#A9A9A6"))
            add(isi(poli("1372,622 1352,588 1498,588 1478,622"), "#C4C3BE"))
            add(isi(poli("1411,588 1418,262 1432,262 1439,588"), "#E4E2DC"))
            add(isi(kotak(1408f, 254f, 34f, 9f), "#B5B4AF"))
            add(isi("M1425 214 Q1445 238 1425 254 Q1405 238 1425 214 Z", "#F2B632"))
        }
        add(isi("M0 650 H1920 V1080 H0 Z", "#3F4E48"))
        add(isi(elips(1340f, 652f, 70f, 22f), "#2F4A3A"))
        add(isi(elips(1510f, 652f, 80f, 24f), "#2F4A3A"))
        add(isi(elips(1420f, 662f, 200f, 18f), "#2F4A3A"))
        add(isi("M0 760 Q480 720 960 750 T1920 736 V1080 H0 Z", "#33423C"))
    }
}
