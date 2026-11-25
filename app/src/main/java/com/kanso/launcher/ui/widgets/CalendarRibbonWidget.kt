package com.kanso.launcher.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.telemetry.CalendarDaySlot
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.WithMechaWidgetTheme
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
import com.kanso.launcher.ui.theme.kansoMechaAccents
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.theme.mechaMetallicChassis

@Composable
fun CalendarRibbonWidget(
    days: List<CalendarDaySlot>,
    mechaTheme: MechaWidgetTheme = MechaWidgetTheme.DEFAULT,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    WithMechaWidgetTheme(mechaTheme) {
        val colors = LocalKansoColors.current
        val isClassic = mechaTheme == MechaWidgetTheme.DEFAULT
        val dayShape = if (isClassic) CircleShape else RoundedCornerShape(6.dp)

        Row(
            modifier = modifier
                .testTag("widget_calendar_ribbon")
                .fillMaxWidth()
                .then(
                    if (isClassic) {
                        Modifier
                            .kansoNeumorphic(raised = true, cornerRadius = 24.dp)
                            .kansoMechaAccents(
                                accentActive = false,
                                showCornerBrackets = true,
                                showLedPip = false
                            )
                    } else {
                        Modifier.mechaMetallicChassis(
                            theme = mechaTheme,
                            darkPlate = false,
                            accentActive = true,
                            cutDp = 11.dp
                        )
                    }
                )
                .kansoTactileClickable(onLongClick = onLongClick, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = if (isClassic) 10.dp else 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            days.forEach { slot ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = slot.dayOfWeekShort,
                        color = if (slot.isToday) colors.inkPrimary else colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = if (slot.isToday) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 9.sp,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(if (isClassic) 5.dp else 3.dp))
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (isClassic) 30.dp else 30.dp,
                                height = if (isClassic) 30.dp else 26.dp
                            )
                            .then(
                                if (slot.isToday) {
                                    Modifier.kansoIconNeumorphic(
                                        cornerRadius = if (isClassic) 15.dp else 6.dp,
                                        shadowScale = 0.55f
                                    )
                                } else Modifier
                            )
                            .clip(dayShape)
                            .background(if (slot.isToday) colors.accent else Color.Transparent)
                            .then(
                                if (!slot.isToday && !isClassic) {
                                    Modifier.border(
                                        width = 0.6.dp,
                                        color = colors.inkSecondary.copy(alpha = 0.16f),
                                        shape = dayShape
                                    )
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = slot.dayOfMonth.toString().padStart(2, '0'),
                            color = if (slot.isToday) colors.onAccent else colors.inkPrimary,
                            fontFamily = if (isClassic) SpaceGroteskFontFamily else JetBrainsMonoFontFamily,
                            fontWeight = if (slot.isToday) FontWeight.Bold else FontWeight.Medium,
                            fontSize = if (isClassic) 13.5.sp else 12.sp
                        )
                    }
                }
            }
        }
    }
}
