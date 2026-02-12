package zebra.BarcodeDetector


import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import zebra.BarcodeDetector.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivitySettingsBinding

    fun onClickbtnCANCELLIST(view: View) {
        binding.tvBARCODESTOHIGHLIGHT.text = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("SettingsPreferences", MODE_PRIVATE)
        binding=ActivitySettingsBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        loadSettings()

        val toggle: ToggleButton = binding.toggleAnalyzerMode
        val current = sharedPreferences.getBoolean("IS_ZEBRA_MODE", false)
        toggle.isChecked = current

        toggle.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("IS_ZEBRA_MODE", isChecked).apply()
        }
    }


    fun onClickbtn_SUBMIT(view: View) {

        sharedPreferences.edit().apply {
            putInt("IMAGESIZE", binding.spinnerIMAGESIZE.selectedItemPosition)
            putInt("SCENE", binding.spinnerSCENE.selectedItemPosition)
            putInt("ZOOM", binding.spinnerZOOM.selectedItemPosition)
            putString("BARCODESTOHIGHLIGHT", binding.tvBARCODESTOHIGHLIGHT.text.toString())
            apply()
        }
        finish()
    }

    private fun loadSettings() {
        val imageSize = sharedPreferences.getInt("IMAGESIZE", 1)
        val scene = sharedPreferences.getInt("SCENE", 0)
        val zoom = sharedPreferences.getInt("ZOOM", 0)
        val barcodesToHighlight = sharedPreferences.getString("BARCODESTOHIGHLIGHT", "")

        binding.spinnerIMAGESIZE.setSelection(imageSize)
        binding.spinnerSCENE.setSelection(scene)
        binding.spinnerZOOM.setSelection(zoom)
        binding.tvBARCODESTOHIGHLIGHT.text = barcodesToHighlight
    }
}