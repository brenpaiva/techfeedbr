package br.com.techfeedbr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.techfeedbr.domain.ContentItem

/** Editorial illustrations for real articles without usable publisher artwork. */
internal data class ThumbnailVariant(val label: String, val icon: ImageVector, val start: Color, val end: Color)

private val variants = listOf(
    ThumbnailVariant("INTELIGÊNCIA ARTIFICIAL", Icons.Outlined.AutoAwesome, Color(0xFF133D79), Color(0xFF3683C5)),
    ThumbnailVariant("CÓDIGO", Icons.Outlined.Code, Color(0xFF0B5153), Color(0xFF12A59A)),
    ThumbnailVariant("NUVEM", Icons.Outlined.Cloud, Color(0xFF315282), Color(0xFF719FD5)),
    ThumbnailVariant("CIBERSEGURANÇA", Icons.Outlined.Security, Color(0xFF263C71), Color(0xFF5967B0)),
    ThumbnailVariant("DADOS", Icons.Outlined.Storage, Color(0xFF145A72), Color(0xFF26A4B8)),
    ThumbnailVariant("MOBILE", Icons.Outlined.Smartphone, Color(0xFF205C6D), Color(0xFF46B4A2)),
    ThumbnailVariant("PROCESSADORES", Icons.Outlined.Memory, Color(0xFF393F76), Color(0xFF8A76C5)),
    ThumbnailVariant("REDES", Icons.Outlined.Hub, Color(0xFF244A72), Color(0xFF58A4CF)),
    ThumbnailVariant("ROBÓTICA", Icons.Outlined.SmartToy, Color(0xFF45627D), Color(0xFF5AB5B1)),
    ThumbnailVariant("ANÁLISE", Icons.Outlined.QueryStats, Color(0xFF155A5D), Color(0xFF5DB5A4)),
    ThumbnailVariant("ESPAÇO", Icons.Outlined.SatelliteAlt, Color(0xFF253C6D), Color(0xFF748BC7)),
    ThumbnailVariant("TERMINAL", Icons.Outlined.Terminal, Color(0xFF194456), Color(0xFF4E9898)),
    ThumbnailVariant("SERVIDORES", Icons.Outlined.Dns, Color(0xFF214E77), Color(0xFF5D9EC0)),
    ThumbnailVariant("WEB", Icons.Outlined.Public, Color(0xFF135C7C), Color(0xFF34A6B8)),
    ThumbnailVariant("DISPOSITIVOS", Icons.Outlined.Devices, Color(0xFF36507B), Color(0xFF6DAAC5)),
    ThumbnailVariant("SOFTWARE", Icons.Outlined.DataObject, Color(0xFF285F6C), Color(0xFF58BCA7)),
    ThumbnailVariant("INFRAESTRUTURA", Icons.Outlined.Lan, Color(0xFF204568), Color(0xFF5488AD)),
    ThumbnailVariant("HARDWARE", Icons.Outlined.DeveloperBoard, Color(0xFF2D537B), Color(0xFF6C9EC8)),
    ThumbnailVariant("PRIVACIDADE", Icons.Outlined.Lock, Color(0xFF315069), Color(0xFF6CB2B3)),
    ThumbnailVariant("INOVAÇÃO", Icons.Outlined.RocketLaunch, Color(0xFF185372), Color(0xFF45A9A9)),
)

private val indicesByCategory = mapOf(
    "Inteligência Artificial" to listOf(0, 8, 9),
    "Desenvolvimento" to listOf(1, 11, 15),
    "Segurança" to listOf(3, 18),
    "Dados" to listOf(4, 9, 15),
    "Mobile" to listOf(5, 14),
    "Cloud e DevOps" to listOf(2, 12, 16),
    "Hardware" to listOf(6, 17),
    "Internet e Apps" to listOf(7, 13, 14),
    "Carreira" to listOf(11, 19),
)

internal fun thumbnailVariantIndex(id: String, categories: Set<String>): Int {
    val themed = indicesByCategory.entries.firstOrNull { it.key in categories }?.value
    return if (themed == null) Math.floorMod(id.hashCode(), variants.size)
    else themed[Math.floorMod(id.hashCode(), themed.size)]
}

@Composable
internal fun TechThumbnail(item: ContentItem, modifier: Modifier = Modifier) {
    val variantIndex = thumbnailVariantIndex(item.id, item.categories)
    val variant = variants[variantIndex]
    Box(modifier.background(Brush.linearGradient(listOf(variant.start, variant.end)))) {
        Canvas(Modifier.fillMaxSize()) {
            val ink = Color.White.copy(alpha = 0.13f)
            when (variantIndex % 5) {
                0 -> repeat(8) { i ->
                    val radius = size.minDimension * (0.10f + i * 0.09f)
                    drawCircle(ink, radius, center = center.copy(x = size.width * 0.82f), style = Stroke(1.5.dp.toPx()))
                }
                1 -> repeat(9) { i ->
                    val x = size.width * i / 7f
                    drawLine(ink, androidx.compose.ui.geometry.Offset(x, 0f),
                        androidx.compose.ui.geometry.Offset(x + size.height * 0.35f, size.height), 2.dp.toPx())
                }
                2 -> repeat(6) { i ->
                    repeat(4) { j ->
                        drawCircle(ink, 3.dp.toPx(),
                            androidx.compose.ui.geometry.Offset(size.width * (0.55f + i * 0.09f), size.height * (0.1f + j * 0.2f)))
                    }
                }
                3 -> repeat(5) { i ->
                    val y = size.height * (0.18f + i * 0.18f)
                    drawLine(ink, androidx.compose.ui.geometry.Offset(size.width * 0.48f, y),
                        androidx.compose.ui.geometry.Offset(size.width, y), 1.dp.toPx())
                    drawCircle(ink, 4.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * (0.65f + (i % 3) * 0.12f), y))
                }
                else -> repeat(5) { i ->
                    val side = size.minDimension * (0.18f + i * 0.14f)
                    drawRect(ink, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.75f - side / 2, size.height * 0.52f - side / 2),
                        size = androidx.compose.ui.geometry.Size(side, side), style = Stroke(1.5.dp.toPx()))
                }
            }
        }
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(variant.icon, contentDescription = null, tint = Color.White,
                    modifier = Modifier.size(38.dp))
                Spacer(Modifier.weight(1f))
                Text("TECHFEED BR", color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }
            Text(variant.label, color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = 14.sp, letterSpacing = 1.1.sp, maxLines = 1,
                modifier = Modifier.fillMaxWidth())
        }
    }
}
