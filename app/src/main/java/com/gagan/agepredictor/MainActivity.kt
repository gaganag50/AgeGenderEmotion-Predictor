package com.gagan.agepredictor

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gagan.agepredictor.databinding.ActivityMainBinding
import com.gagan.agepredictor.fragments.AboutFragment
import com.gagan.agepredictor.fragments.MainFragment
import com.gagan.agepredictor.utils.NavigationHelper.Companion.MAIN_FRAGMENT_TAG
import com.github.dhaval2404.imagepicker.ImagePicker
import es.dmoral.toasty.Toasty
import java.io.File
import com.gagan.agepredictor.fragments.DetailsFragment as DetailsFragment1

class MainActivity : AppCompatActivity(), MainFragment.OnItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val fragment = MainFragment()
        fragmentTransaction.add(R.id.fragment_container, fragment)
        fragmentTransaction.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.camera -> {
                clickPhoto()
                true
            }
            R.id.about -> {
                replaceFragment(AboutFragment())
                true
            }


            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> {
                val filePath: String = ImagePicker.getFilePath(data)!!
                val fragment = DetailsFragment1()
                val bundle = Bundle()
                bundle.putString("filePath", filePath)

                fragment.arguments = bundle
                replaceFragment(fragment)
            }
            ImagePicker.RESULT_ERROR -> {
                Toasty.error(this, ImagePicker.getError(data), Toast.LENGTH_SHORT,true).show()
            }
            else -> {
                Toasty.info(this, "Task Cancelled", Toast.LENGTH_SHORT,true).show()
            }
        }
    }

    private fun clickPhoto() {
        ImagePicker.with(this)
            .crop()                    //Crop image(Optional), Check Customization for more option
            .compress(1024)            //Final image size will be less than 1 MB(Optional)
            .maxResultSize(
                1080,
                1080
            )    //Final image resolution will be less than 1080 x 1080(Optional)
            .start()
    }

    companion object {
//        const val TAG = "GAGAN"

        val data = listOf(

            R.drawable.pic1,
            R.drawable.pic2,
            R.drawable.pic3,
            R.drawable.pic4,
            R.drawable.pic5,
            R.drawable.pic6,
            R.drawable.pic7,
            R.drawable.pic8,
            R.drawable.pic9,
            R.drawable.pic10
        )

        val emojis = mapOf(
            Pair("Angry", R.drawable.angry),
            Pair("Fear", R.drawable.fear),
            Pair("Happy", R.drawable.happy),
            Pair("Neutral", R.drawable.neutral),
            Pair("Sad", R.drawable.sad),
            Pair("Surprise", R.drawable.surprise)
        )
        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }


    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.addToBackStack(MAIN_FRAGMENT_TAG)
        fragmentTransaction.commit()
    }

    override fun onItemSelected(position: Int) {
        val fragment = DetailsFragment1()
        val bundle = Bundle()
        bundle.putInt("position", position)
        fragment.arguments = bundle
        replaceFragment(fragment)
    }
}