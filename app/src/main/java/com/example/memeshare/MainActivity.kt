package com.example.sharememes

import android.Manifest
import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.memeshare.R
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Math.abs


class MainActivity : AppCompatActivity() , GestureDetector.OnGestureListener{

    lateinit var gestureDetector: GestureDetector              // for onTouchEvent
    var x1:Float=0.0f
    var x2:Float=0.0f
    var y1:Float=0.0f
    var y2:Float=0.0f

//    companion object{
//        const val MIN_DISTANCE = 150
//    }

    var currentMemeUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadMeme()

        gestureDetector = GestureDetector(this,this)  ///   for onTouchEvent


    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_item,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
//            R.id.logout ->{
//
//                /////  Alert Dialog Box
//
//                Toast.makeText(this,"LOGOUT....",Toast.LENGTH_LONG).show()
//                val builder = AlertDialog.Builder(this)
//                builder.setTitle("LOGOUT")
//                builder.setMessage("Are You Sure ")
//                builder.setIcon(R.drawable.logout)
//                builder.setPositiveButton("Yes"){dialogInterface, which ->
//                    Toast.makeText(applicationContext,"clicked yes",Toast.LENGTH_LONG).show()
//                }
//                builder.setNegativeButton("No"){dialogInterface, which ->
//                    Toast.makeText(applicationContext,"clicked No",Toast.LENGTH_LONG).show()
//                }
//                val alertDialog: AlertDialog = builder.create()
//                alertDialog.setCancelable(false)
//                alertDialog.show()
//                true
//            }
            R.id.download ->{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    askPermissions()
                } else {
//                    Toast.makeText(this,"ELSE DOWNLOAD....",Toast.LENGTH_LONG).show()
                    Toast.makeText(this,"DOWNLOADING   ${currentMemeUrl}", Toast.LENGTH_LONG).show()
                    downloadImage(currentMemeUrl.toString())
                }
                //               Toast.makeText(this,"DOWNLOADING ${currentMemeUrl}",Toast.LENGTH_LONG).show()

                true
            }else->super.onOptionsItemSelected(item)
        }

    }

    var msg: String? = ""
    var lastMsg = ""

    fun downloadImage(url: String) {
        val directory = File(Environment.DIRECTORY_PICTURES)

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val downloadManager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadUri = Uri.parse(url)

        val request = DownloadManager.Request(downloadUri).apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(url.substring(url.lastIndexOf("/") + 1))
                .setDescription("")
                .setDestinationInExternalPublicDir(
                    directory.toString(),
                    url.substring(url.lastIndexOf("/") + 1)
                )
        }
        val downloadId = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)
        Thread(Runnable {
            var downloading = true
            while (downloading) {
                val cursor: Cursor = downloadManager.query(query)
                cursor.moveToFirst()
                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                    downloading = false
                }
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                msg = statusMessage(url, directory, status)
                if (msg != lastMsg) {
                    this.runOnUiThread {
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                    lastMsg = msg ?: ""
                }
                cursor.close()
            }
        }).start()
    }

    private fun statusMessage(url: String, directory: File, status: Int): String? {
        var msg = ""
        msg = when (status) {
            DownloadManager.STATUS_FAILED -> "Download has been failed, please try again"
            DownloadManager.STATUS_PAUSED -> "Paused"
            DownloadManager.STATUS_PENDING -> "Pending"
            DownloadManager.STATUS_RUNNING -> "Downloading..."
            DownloadManager.STATUS_SUCCESSFUL -> "Image downloaded successfully in $directory" + File.separator + url.substring(
                url.lastIndexOf("/") + 1
            )
            else -> "There's nothing to download"
        }
        return msg
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun askPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder(this)
                    .setTitle("Permission required")
                    .setMessage("Permission required to save photos from the Web.")
                    .setPositiveButton("Accept") { dialog, id ->
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
                        finish()
                    }
                    .setNegativeButton("Deny") { dialog, id -> dialog.cancel() }
                    .show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)


            }
        } else {
            // Permission has already been granted
            downloadImage(currentMemeUrl.toString())
        }
    }


    companion object {
        private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay!
                    // Download the Image
                    downloadImage(currentMemeUrl.toString())
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return
            }
            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }






    private fun loadMeme() {
        //nextButton.isEnabled = false
        //shareButton.isEnabled = false
        progressBar.visibility = View.VISIBLE


        val url = "https://meme-api.herokuapp.com/gimme"
        // Request a string response from the provided URL.
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                currentMemeUrl = response.getString("url")

                Glide.with(this).load(currentMemeUrl).listener(object : RequestListener<Drawable>{
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        //nextButton.isEnabled = true
                        //shareButton.isEnabled = true
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }
                }).into(memeImageView)
            },
            {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
            })

        // Add the request to the RequestQueue.
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }

    fun showNextMeme(view: View) {
        loadMeme()
    }



    fun shareMeme() {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, "Hi, checkout this meme $currentMemeUrl")
        startActivity(Intent.createChooser(i, "Share this meme with"))
        Toast.makeText(this,"SHAREING  ${currentMemeUrl}",Toast.LENGTH_LONG).show()


//        Picasso.with(this).load(currentMemeUrl).into(memeImageView)
//        val image: Bitmap? = getBitmapFromView(memeImageView)
//
//        val i = Intent(Intent.ACTION_SEND)
//        i.type = "image/*"
//        i.putExtra(Intent.EXTRA_STREAM, getimageuri( this, image!!) )
//        startActivity(Intent.createChooser(i, "Share this meme with ..."))
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {

        gestureDetector.onTouchEvent(event)

        when (event?.action){
            0->{
                x1=event.x
                y1=event.y
            }
            1->{
                x2=event.x
                y2=event.y
                val valueX:Float=x2-x1
                val valueY:Float=y2-y1
                if(abs(valueX)>10){
                    if(x2>x1){
                        Toast.makeText(this,"Right Swap",Toast.LENGTH_LONG).show()

                    }else{
                        Toast.makeText(this,"Left Swap",Toast.LENGTH_LONG).show()
                        loadMeme()

                    }
                }else{
                    if(y2>y1){
                        Toast.makeText(this,"Bottom Swap",Toast.LENGTH_LONG).show()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            askPermissions()
                        } else {
//                    Toast.makeText(this,"ELSE DOWNLOAD....",Toast.LENGTH_LONG).show()
                            Toast.makeText(this,"DOWNLOADING ${currentMemeUrl}",Toast.LENGTH_LONG).show()
                            downloadImage(currentMemeUrl.toString())
                        }


                    }else{
                        Toast.makeText(this,"Top Swap",Toast.LENGTH_LONG).show()
                        shareMeme()
                    }
                }

            }

        }
        return super.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent?): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    override fun onShowPress(e: MotionEvent?) {
        //TODO("Not yet implemented")

    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
        //TODO("Not yet implemented")

    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        //TODO("Not yet implemented")
        return false
    }



//    private fun getimageuri(inContext: Context,inImage: Bitmap): String? {
//        val bytes=ByteArrayOutputStream()
//        inImage.compress(Bitmap.CompressFormat.JPEG,100,bytes)
//        val path= MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "title",null)
//
//        return Uri.parse(path).toString()
//    }
//
//
//    private fun getBitmapFromView(memeImageView: ImageView): Bitmap? {
//        val bitmap=Bitmap.createBitmap(memeImageView.width ,memeImageView.height, Bitmap.Config.ARGB_8888)
//        val canvas =Canvas(bitmap)
//        memeImageView.draw(canvas)
//        return bitmap
//    }
}

