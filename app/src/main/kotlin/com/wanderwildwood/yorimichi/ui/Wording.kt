package com.wanderwildwood.yorimichi.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.yorimichi.R
import com.wanderwildwood.yorimichi.walk.Age
import com.wanderwildwood.yorimichi.walk.Bearing
import com.wanderwildwood.yorimichi.walk.Distance
import com.wanderwildwood.yorimichi.walk.FixLine
import com.wanderwildwood.yorimichi.walk.RadiusLabel
import com.wanderwildwood.yorimichi.walk.Reading
import com.wanderwildwood.yorimichi.walk.oneDecimal

// The words for what walk/Words.kt decided. The deciding is tested there without a phone;
// the wording lives here, next to the resources, so a translation can reach all of it.
//
// Numbers go into the strings already written out, rather than as %d, so that they stay
// in the digits they have always been in whatever language the phone is set to.

@Composable
fun said(distance: Distance): String = when (distance) {
    is Distance.Feet -> stringResource(R.string.distance_feet, distance.feet.toString())
    is Distance.Miles -> stringResource(R.string.distance_miles, oneDecimal(distance.miles))
    is Distance.Metres -> stringResource(R.string.distance_metres, distance.metres.toString())
    is Distance.Kilometres -> stringResource(R.string.distance_kilometres, oneDecimal(distance.kilometres))
}

@Composable
fun said(label: RadiusLabel): String = when (label) {
    is RadiusLabel.Metres -> stringResource(R.string.distance_metres, label.metres.toString())
    is RadiusLabel.Kilometres -> stringResource(R.string.distance_kilometres, label.kilometres.toString())
    is RadiusLabel.Miles -> pluralStringResource(R.plurals.radius_miles, label.miles, label.miles.toString())
    RadiusLabel.QuarterMile -> stringResource(R.string.radius_quarter_mile)
    RadiusLabel.HalfMile -> stringResource(R.string.radius_half_mile)
}

@Composable
fun said(bearing: Bearing): String =
    stringResource(R.string.bearing, stringResource(bearing.point.labelRes), bearing.degrees.toString())

@Composable
fun said(reading: Reading): String = when (reading) {
    is Reading.Thicker -> stringResource(R.string.reading_thicker, oneDecimal(reading.times))
    Reading.LittleThicker -> stringResource(R.string.reading_little_thicker)
    Reading.Ordinary -> stringResource(R.string.reading_ordinary)
    Reading.LittleThinner -> stringResource(R.string.reading_little_thinner)
    is Reading.Thinner -> stringResource(R.string.reading_thinner, oneDecimal(reading.times))
}

@Composable
fun said(line: FixLine): String = when (line) {
    FixLine.Waiting -> stringResource(R.string.fix_waiting)
    is FixLine.Stale -> stringResource(R.string.fix_stale, said(line.age))
    FixLine.UnknownAccuracy -> stringResource(R.string.fix_unknown_accuracy)
    is FixLine.GoodTo -> stringResource(R.string.fix_good_to, said(line.accuracy))
}

@Composable
fun said(age: Age): String = when (age) {
    is Age.Minutes -> stringResource(R.string.age_minutes, age.minutes.toString())
    is Age.Hours -> stringResource(R.string.age_hours, age.hours.toString())
    is Age.Days -> pluralStringResource(R.plurals.age_days, age.days.toInt(), age.days.toString())
}
