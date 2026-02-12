package zebra.BarcodeDetector

import android.graphics.Paint

data class BCEvent(val xavg: Float, val yavg: Float, val paint: Paint, val bcValue:String, val trackingID: String, val timestamp: Long) {

}
