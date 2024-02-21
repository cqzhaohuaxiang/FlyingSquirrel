package com.android.umbrellacorporation.tools


import android.util.Log
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**鼠标移动处理
 * 目前只采用了比例与微分方式
 * 以后采用运动预测方式
 * */
class MouseMovementHandling {
    private var pointerStartX = 0f                                    //最先X坐标
    private var pointerStartY = 0f                                   //最先Y坐标
    private var wheelStartX = 0f
    private var wheelStartY = 0f
    private var mouseTimeStamp = 0L                         //时间戳
    private var mousePointerBegin = false       //指针运动
    private var mouseWheelBegin = false  //滚轮运动

    /**一根手指移动  鼠标指针运动中
     *
     * standardization 参数为图像与你之间的距离比例 远大近小
     *
     * 触摸时 standardization = 1
     * */
     fun mousePointerMove(x:Float,y:Float ,timeStamp:Long,sensitivity:Int,velocity:Int ,standardization:Float):ByteArray{

        val direction = ByteArray(2)
        if(mousePointerBegin && !mouseWheelBegin){
            val derivative : Float = (1000 - (timeStamp -mouseTimeStamp))/1000f
            if(abs(x - pointerStartX ) > sensitivity){
                /**左右*/
                var xMoves = ((abs(x - pointerStartX ) - sensitivity) * velocity * standardization).toInt()
                if (xMoves > 125 )  xMoves = 125
                if((x - pointerStartX) > 0 ){
                    direction[0] = (xMoves * derivative ).toInt().toByte()

                }else{
                    direction[0] = (-1 * xMoves * derivative ).toInt().toByte()
                }
            }
            if(abs(y - pointerStartY ) > sensitivity){
                /**上下*/
                var yMoves = ((abs(y - pointerStartY) - sensitivity) * velocity* standardization).toInt()
                if (yMoves > 125 ) yMoves = 125
                if ((y - pointerStartY) > 0){
                    direction[1] = (yMoves * derivative ).toInt().toByte()
                }else {
                    direction[1] = (-1 * yMoves * derivative ).toInt().toByte()
                }
            }
            pointerStartX = x
            pointerStartY = y
            mouseTimeStamp = timeStamp
            return  direction
        }else {
            pointerStartX = x
            pointerStartY = y
            mouseTimeStamp = timeStamp
            mousePointerBegin = true
        }
        return direction
    }

    /**两根手指移动  滚轮运动中
     *
     * 判定的移动的阀值为 食指与中指间的距离 （动态的 不固定）
     * */
     fun mouseWheelMove(x:Float,y:Float,x1:Float,y1:Float,wheel : Int):ByteArray{
        val direction = ByteArray(2)
        if (mouseWheelBegin && !mousePointerBegin){
            val thresholds = sqrt(abs(((x - x1).pow(2)) + abs((y - y1).pow(2))))//双指间的距离
            val move =  sqrt(abs(((x - wheelStartX).pow(2)) + abs((y - wheelStartY).pow(2)))) //这次移动的距离
            if(move > thresholds){
                if(abs(x - wheelStartX) > abs(y - wheelStartY )){
                    if ((x - wheelStartX) > 0) direction[0] = (1 * wheel).toByte() else direction[0] = (-1 * wheel).toByte()
                }else {
                    if ((y - wheelStartY ) > 0) direction[1] = (-1 * wheel).toByte() else direction[1] = (1 * wheel).toByte()
                }
                wheelStartX = x
                wheelStartY = y

                return direction
            }
        }else{
            wheelStartX = x
            wheelStartY = y
            mouseWheelBegin = true
        }
        return direction
    }

    /**所有运动离开了*/
    fun allMoveLeave(){
        mousePointerBegin = false
        mouseWheelBegin = false

    }


}