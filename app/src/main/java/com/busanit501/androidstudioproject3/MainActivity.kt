package com.example.test1

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.firebaseexample.R
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var eventList: LinearLayout
    private lateinit var button1: Button

    private lateinit var database: DatabaseReference

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        eventList = findViewById(R.id.event_list)
        button1 = findViewById(R.id.button1)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().reference.child("events")

        // Display saved events
        displayEvents()

        // Set click listener for button1 to navigate to another activity
        button1.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayEvents() {
        eventList.removeAllViews() // Clear existing views
        database.get().addOnSuccessListener { snapshot ->
            for (eventSnapshot in snapshot.children) {
                val event = eventSnapshot.getValue(Event::class.java)
                event?.let { event ->
                    val eventView = layoutInflater.inflate(R.layout.event_item, null)
                    val nameView = eventView.findViewById<TextView>(R.id.event_name_view)
                    val descriptionView = eventView.findViewById<TextView>(R.id.event_description_view)
                    val dateView = eventView.findViewById<TextView>(R.id.event_date_view)
                    val timeView = eventView.findViewById<TextView>(R.id.event_time_view)
                    val locationView = eventView.findViewById<TextView>(R.id.event_location_view)
                    val countView = eventView.findViewById<TextView>(R.id.event_count_view)
                    val imageView = eventView.findViewById<ImageView>(R.id.event_image_view)
                    val deleteButton = eventView.findViewById<Button>(R.id.delete_event_button)
                    val editButton = eventView.findViewById<Button>(R.id.edit_event_button)

                    nameView.text = "제목: ${event.name}"
                    descriptionView.text = "글쓴이: ${event.description}"
                    dateView.text = "내용: ${event.date}"
                    timeView.text = "작성시간: ${event.time}"
                    countView.text = "번호: ${event.count}"
//                    locationView.text = "장소: ${event.location}"


                    if (event.imageUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(event.imageUrl)
                            .into(imageView)
                        imageView.visibility = View.VISIBLE
                    } else {
                        imageView.visibility = View.GONE
                    }

                    // Set click listener to open EventDetailActivity
                    eventView.setOnClickListener {
                        val intent = Intent(this, EventDetailActivity::class.java)
                        intent.putExtra("eventName", event.name)
                        intent.putExtra("eventDescription", event.description)
                        intent.putExtra("eventDate", event.date)
                        intent.putExtra("eventTime", event.time)
                        intent.putExtra("eventImageUrl", event.imageUrl)
                        intent.putExtra("eventCount", event.count)  // Pass the event count
                        startActivity(intent)
                    }

                    // Set click listener for the delete button
                    deleteButton.setOnClickListener {
                        deleteEvent(event.count)
                    }

                    // Set click listener for the edit button
                    editButton.setOnClickListener {
                        showUpdateDialog(event)
                    }

                    eventList.addView(eventView)
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "이벤트를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteEvent(eventNumber: Int) {
        database.get().addOnSuccessListener { snapshot ->
            for (eventSnapshot in snapshot.children) {
                val event = eventSnapshot.getValue(Event::class.java)
                if (event?.count == eventNumber) {
                    eventSnapshot.ref.removeValue().addOnSuccessListener {
                        Toast.makeText(this, "이벤트가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        displayEvents() // Refresh the event list after deletion
                    }.addOnFailureListener {
                        Toast.makeText(this, "이벤트 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                    return@addOnSuccessListener // Exit the loop after deleting the event
                }
            }
            Toast.makeText(this, "해당 번호의 이벤트를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "이벤트를 삭제하는 동안 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUpdateDialog(event: Event) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_update_event, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.update_event_name)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.update_event_description)
        val dateEditText = dialogView.findViewById<EditText>(R.id.update_event_date)
        val timeEditText = dialogView.findViewById<EditText>(R.id.update_event_time)
//        val locationEditText = dialogView.findViewById<EditText>(R.id.update_event_location)

        // Pre-fill existing event data
        nameEditText.setText(event.name)
        descriptionEditText.setText(event.description)
        dateEditText.setText(event.date)
        timeEditText.setText(event.time)
//        locationEditText.setText(event.location)

        AlertDialog.Builder(this)
            .setTitle("게시글 수정")
            .setView(dialogView)
            .setPositiveButton("수정") { _, _ ->
                val updatedName = nameEditText.text.toString()
                val updatedDescription = descriptionEditText.text.toString()
                val updatedDate = dateEditText.text.toString()
                val updatedTime = timeEditText.text.toString()
//                val updatedLocation = locationEditText.text.toString()

                if (updatedName.isEmpty() || updatedDescription.isEmpty() || updatedDate.isEmpty() || updatedTime.isEmpty() ) {
                    Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedEvent = event.copy(
                    name = updatedName,
                    description = updatedDescription,
                    date = updatedDate,
                    time = updatedTime,
//                    location = updatedLocation
                )

                updateEvent(event.count, updatedEvent)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateEvent(eventNumber: Int, updatedEvent: Event) {
        database.get().addOnSuccessListener { snapshot ->
            for (eventSnapshot in snapshot.children) {
                val event = eventSnapshot.getValue(Event::class.java)
                if (event?.count == eventNumber) {
                    eventSnapshot.ref.setValue(updatedEvent)
                        .addOnSuccessListener {
                            Toast.makeText(this, "이벤트가 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                            displayEvents() // Refresh the event list after updating
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "이벤트 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    return@addOnSuccessListener // Exit the loop after updating the event
                }
            }
            Toast.makeText(this, "해당 번호의 이벤트를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "이벤트를 업데이트하는 동안 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
