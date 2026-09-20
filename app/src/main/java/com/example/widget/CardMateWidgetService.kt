package com.example.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.model.BusinessCard
import kotlinx.coroutines.runBlocking

class CardMateWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CardMateRemoteViewsFactory(applicationContext)
    }
}

class CardMateRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var cardsList: List<BusinessCard> = emptyList()

    override fun onCreate() {
        fetchCards()
    }

    override fun onDataSetChanged() {
        fetchCards()
    }

    private fun fetchCards() {
        try {
            val database = AppDatabase.getInstance(context)
            cardsList = database.businessCardDao().getRecentAndFavoriteCardsSync()
        } catch (e: Exception) {
            e.printStackTrace()
            cardsList = emptyList()
        }
    }

    override fun onDestroy() {
        cardsList = emptyList()
    }

    override fun getCount(): Int = cardsList.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= cardsList.size) {
            return RemoteViews(context.packageName, R.layout.widget_card_item)
        }

        val card = cardsList[position]
        val views = RemoteViews(context.packageName, R.layout.widget_card_item)

        // 1. Initial letter
        val initial = if (card.fullName.isNotBlank()) card.fullName.trim().first().uppercase() else "C"
        views.setTextViewText(R.id.widget_item_initial, initial)

        // 2. Full Name
        views.setTextViewText(R.id.widget_item_name, card.fullName)

        // 3. Category
        val categoryLabel = if (card.isFavorite) "★ ${card.category}" else card.category
        views.setTextViewText(R.id.widget_item_category, categoryLabel)

        // 4. Job Title & Company
        val subtitle = when {
            card.jobTitle.isNotBlank() && card.company.isNotBlank() -> "${card.jobTitle} • ${card.company}"
            card.company.isNotBlank() -> card.company
            card.jobTitle.isNotBlank() -> card.jobTitle
            else -> "Digital Business Card"
        }
        views.setTextViewText(R.id.widget_item_title_company, subtitle)

        // 5. Contact (Phone or Email)
        val contactInfo = when {
            card.phone.isNotBlank() -> card.phone
            card.email.isNotBlank() -> card.email
            else -> card.address
        }
        views.setTextViewText(R.id.widget_item_phone, contactInfo)

        // 6. Fill-in intent with card id for onClick
        val fillInIntent = Intent().apply {
            putExtra(CardMateWidgetProvider.EXTRA_CARD_ID, card.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position in cardsList.indices) cardsList[position].id else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
