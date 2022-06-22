package app.mango.music.ui

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.ln

private const val swipeReboundingElasticity = 0.8F

private const val trueSwipeThreshold = 0.4F

class ReboundingSwipeActionCallback : ItemTouchHelper.SimpleCallback(
    0,
    ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT
) {

    interface ReboundableViewHolder {

        val reboundableView: View

        fun onReboundOffsetChanged(
            currentSwipePercentage: Float,
            swipeThreshold: Float,
            currentTargetHasMetThresholdOnce: Boolean,
            dampedCoefficient:Float
        )

        fun onReboundedFavorite()
        fun onReboundedNext()
    }

    private var dampedCoefficient = 1F
    private var currentTargetPosition: Int = -1
    private var currentTargetHasMetThresholdOnce: Boolean = false

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = Float.MAX_VALUE

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return Float.MAX_VALUE
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return Float.MAX_VALUE
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {

        if (currentTargetHasMetThresholdOnce && viewHolder is ReboundableViewHolder){
            currentTargetHasMetThresholdOnce = false
            if(dampedCoefficient>0)viewHolder.onReboundedFavorite()
            else viewHolder.onReboundedNext()
        }
        super.clearView(recyclerView, viewHolder)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (viewHolder !is ReboundableViewHolder) return
        if (currentTargetPosition != viewHolder.absoluteAdapterPosition) {
            currentTargetPosition = viewHolder.absoluteAdapterPosition
            currentTargetHasMetThresholdOnce = false
        }

        val itemView = viewHolder.itemView
        val currentSwipePercentage = abs(dX) / itemView.width
        viewHolder.onReboundOffsetChanged(
            currentSwipePercentage,
            trueSwipeThreshold,
            currentTargetHasMetThresholdOnce,
            dampedCoefficient
        )
        translateReboundingView(itemView, viewHolder, dX,isCurrentlyActive)

        if (currentSwipePercentage >= trueSwipeThreshold &&
            !currentTargetHasMetThresholdOnce) {
            currentTargetHasMetThresholdOnce = true
        }
    }

    private fun translateReboundingView(
        itemView: View,
        viewHolder: ReboundableViewHolder,
        dX: Float,
        isCurrentlyActive:Boolean
    ) {

        dampedCoefficient = if(isCurrentlyActive&&dX>0) 1F
        else if(isCurrentlyActive&&dX<0) -1F
        else dampedCoefficient
        
        val swipeDismissDistanceHorizontal = itemView.width * trueSwipeThreshold
        val dragFraction = ln(
            (1 + (dampedCoefficient*dX / swipeDismissDistanceHorizontal)).toDouble()) / dampedCoefficient*ln(3.toDouble()
        )
        val dragTo = dragFraction * swipeDismissDistanceHorizontal *
            swipeReboundingElasticity

        viewHolder.reboundableView.translationX = dragTo.toFloat()
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

    }
}