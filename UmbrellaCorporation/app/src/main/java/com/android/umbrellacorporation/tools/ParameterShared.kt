package com.android.umbrellacorporation.tools

import android.content.Context
import android.content.SharedPreferences

/**运行时的一些参数保存与提取*/
class ParameterShared (context: Context) {
    private var parameter : SharedPreferences
    private var editor : SharedPreferences.Editor
    private var context : Context
    init {
        this.context = context
        parameter = context.getSharedPreferences("parameter",Context.MODE_PRIVATE)
        editor = parameter.edit()


    }


    fun getParameter(name : String ,type : Any) :Any {
        /**先检查一下type的类型
         * 目前只有Boolean 和 Int 类型
         * **/
        if(type is Boolean) {
            return parameter.getBoolean(name, type)
        }
        else if (type is Int) {
            return parameter.getInt(name,type)
        }

        return false
    }


    fun setParameter(name : String ,type : Any){
        if(type is Boolean) {
            editor.putBoolean(name, type)
            editor.apply() //异步提交数据
        }
        if (type is Int) {
            editor.putInt(name, type)
            editor.apply()
        }
    }
}