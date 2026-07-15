package de.tobias.lgacremote

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private var irManager: ConsumerIrManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        irManager = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    RemoteScreen(
                        hasIr = irManager?.hasIrEmitter() == true,
                        onTransmit = { code, lg2 -> transmit(code, lg2) }
                    )
                }
            }
        }
    }

    private fun transmit(code: Long, lg2: Boolean) {
        val ir = irManager
        if (ir == null || !ir.hasIrEmitter()) {
            Toast.makeText(this, "Kein IR-Blaster gefunden", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            ir.transmit(LgAcEncoder.CARRIER_FREQ_HZ, LgAcEncoder.toPattern(code, lg2))
            haptic()
        } catch (e: Exception) {
            Toast.makeText(this, "Sendefehler: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun haptic() {
        val vib = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= 26)
            vib.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vib.vibrate(30)
    }
}

@Composable
fun RemoteScreen(hasIr: Boolean, onTransmit: (Long, Boolean) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("state", Context.MODE_PRIVATE) }

    var temp by remember { mutableIntStateOf(prefs.getInt("temp", 22)) }
    var mode by remember { mutableIntStateOf(prefs.getInt("mode", LgAcEncoder.MODE_COOL)) }
    var fan by remember { mutableIntStateOf(prefs.getInt("fan", LgAcEncoder.FAN_AUTO)) }
    var lg2 by remember { mutableStateOf(prefs.getBoolean("lg2", true)) }
    var isOn by remember { mutableStateOf(false) }

    fun save() = prefs.edit().putInt("temp", temp).putInt("mode", mode)
        .putInt("fan", fan).putBoolean("lg2", lg2).apply()

    fun sendState() {
        isOn = true
        onTransmit(LgAcEncoder.buildState(mode, temp, fan), lg2)
        save()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("LG Klima", style = MaterialTheme.typography.headlineMedium)
        if (!hasIr) {
            Spacer(Modifier.height(8.dp))
            Text("⚠ Kein IR-Blaster verfügbar", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))

        // Temperaturanzeige
        Text(
            "$temp°C",
            fontSize = 72.sp,
            fontWeight = FontWeight.Light
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledTonalButton(
                onClick = { if (temp > LgAcEncoder.MIN_TEMP) { temp--; sendState() } },
                modifier = Modifier.size(72.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) { Text("−", fontSize = 28.sp) }
            FilledTonalButton(
                onClick = { if (temp < LgAcEncoder.MAX_TEMP) { temp++; sendState() } },
                modifier = Modifier.size(72.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) { Text("+", fontSize = 28.sp) }
        }

        Spacer(Modifier.height(24.dp))

        // Ein / Aus
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { sendState() },
                modifier = Modifier.weight(1f).height(56.dp)
            ) { Text("EIN / Senden") }
            OutlinedButton(
                onClick = { isOn = false; onTransmit(LgAcEncoder.OFF_COMMAND, lg2) },
                modifier = Modifier.weight(1f).height(56.dp)
            ) { Text("AUS") }
        }

        Spacer(Modifier.height(24.dp))

        // Modus
        Text("Modus", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        val modes = listOf(
            "Kühlen" to LgAcEncoder.MODE_COOL,
            "Entfeuchten" to LgAcEncoder.MODE_DRY,
            "Lüfter" to LgAcEncoder.MODE_FAN,
            "Auto" to LgAcEncoder.MODE_AUTO,
            "Heizen" to LgAcEncoder.MODE_HEAT
        )
        FlowRowSegmented(modes, mode) { mode = it; sendState() }

        Spacer(Modifier.height(16.dp))

        // Lüfter
        Text("Lüfterstufe", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        val fans = listOf(
            "Min" to LgAcEncoder.FAN_LOWEST,
            "Niedrig" to LgAcEncoder.FAN_LOW,
            "Mittel" to LgAcEncoder.FAN_MEDIUM,
            "Max" to LgAcEncoder.FAN_MAX,
            "Auto" to LgAcEncoder.FAN_AUTO
        )
        FlowRowSegmented(fans, fan) { fan = it; sendState() }

        Spacer(Modifier.height(16.dp))

        // Swing + Licht
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onTransmit(LgAcEncoder.SWING_V_TOGGLE, lg2) },
                modifier = Modifier.weight(1f)) { Text("Swing") }
            OutlinedButton(onClick = { onTransmit(LgAcEncoder.LIGHT_TOGGLE, lg2) },
                modifier = Modifier.weight(1f)) { Text("Display") }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // Protokollvariante
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LG2-Timing", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (lg2) "Moderne Geräte (Standard)" else "Klassisches Timing (ältere Geräte)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = lg2, onCheckedChange = { lg2 = it; save() })
        }
    }
}

@Composable
fun FlowRowSegmented(items: List<Pair<String, Int>>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                row.forEach { (label, value) ->
                    FilterChip(
                        selected = selected == value,
                        onClick = { onSelect(value) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}
