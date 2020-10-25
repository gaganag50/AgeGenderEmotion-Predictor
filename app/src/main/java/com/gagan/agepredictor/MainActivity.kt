package com.gagan.agepredictor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.gagan.agepredictor.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), MasterFragment.onItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val fragment = MasterFragment()
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
            R.id.detect -> {
                predict()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun predict() {

    }

    private fun clickPhoto() {

    }

    companion object {
        const val TAG = "GAGAN"
        val data = listOf(
            R.drawable.black, R.drawable.boy, R.drawable.couple,
            R.drawable.randeep_couple,
            R.drawable.ben,
            R.drawable.plain,
            R.drawable.family,
            R.drawable.tony_resized,
            R.drawable.shawn_resized,
            R.drawable.arjit_resized
        )
    }

    override fun onItemSelected(position: Int) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val fragment = DetailsFragment()
        val bundle = Bundle()
        bundle.putInt("position", position)
        fragment.arguments = bundle
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()


    }
}