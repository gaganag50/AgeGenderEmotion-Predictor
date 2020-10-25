package com.gagan.agepredictor

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.gagan.agepredictor.databinding.ActivityMainBinding
import com.github.dhaval2404.imagepicker.ImagePicker
import com.gagan.agepredictor.DetailsFragment as DetailsFragment1

class MainActivity : AppCompatActivity(), TutorialFragment.onItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val fragment = TutorialFragment()
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
                Log.d(TAG, "onActivityResult: bundle ${bundle}")
                Log.d(TAG, "onActivityResult: bundle ${bundle.get("position")}")
                Log.d(TAG, "onActivityResult: bundle ${bundle.get("filePath")}")
                fragment.arguments = bundle
                replaceFragment(fragment)
            }
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
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
        const val TAG = "GAGAN"
        val data = listOf(

            R.drawable.pic4,
            R.drawable.pic3,
            R.drawable.pic6

//
//            R.drawable.black, R.drawable.boy, R.drawable.couple,
//            R.drawable.randeep_couple,
//            R.drawable.ben,
//            R.drawable.plain,
//            R.drawable.family,
//            R.drawable.tony_resized,
//            R.drawable.shawn_resized,
//            R.drawable.arjit_resized
        )
    }

    fun replaceFragment(fragment:Fragment){
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.addToBackStack(null)
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