package com.android.umbrellacorporation.tools

import android.graphics.Path

/**svg数据生成路径*/
class PathGeneration {

    /**SVG 路径绘图      请使和 inkscape 软件输出SVG数据
     * divisor 为图片缩小比例
     * */
     fun getpath(data :String, divisor : Float) : Path {
        val path = Path()
        var lastNumber = 0                //上次位置
        var presentNumber = 0             //现在位置
        var lastCommand = data[0]        //上次SVG的命令
        var presentCommand = data[0]     //当前SVG的命令
        var valid = false
        var relativelyX = 0f
        var relativelyY = 0f

//        val pathMeasure = PathMeasure(path, false)
//        path.moveTo(relativelyX, relativelyY)//设置Path的起点

        for (index  in 1..data.lastIndex ){
            /**当有效命令时**/
            when(data[index]){
                'M' -> {
                    presentNumber = index
                    presentCommand = 'M'
                    valid = true
                }
                'm' -> {
                    presentNumber = index
                    presentCommand = 'm'
                    valid = true
                }
                'L' -> {
                    presentNumber = index
                    presentCommand = 'L'
                    valid = true
                }
                'l' -> {
                    presentNumber = index
                    presentCommand = 'l'
                    valid = true
                }
                'H' -> {
                    presentNumber = index
                    presentCommand = 'H'
                    valid = true
                }
                'h' -> {
                    presentNumber = index
                    presentCommand = 'h'
                    valid = true
                }
                'V' -> {
                    presentNumber = index
                    presentCommand = 'V'
                    valid = true
                }
                'v' -> {
                    presentNumber = index
                    presentCommand = 'v'
                    valid = true
                }
                'C' -> {
                    presentNumber = index
                    presentCommand = 'C'
                    valid = true
                }
                'c' -> {
                    presentNumber = index
                    presentCommand = 'c'
                    valid = true
                }
                'S' -> {
                    presentNumber = index
                    presentCommand = 'S'
                    valid = true
                }
                's' -> {
                    presentNumber = index
                    presentCommand = 's'
                    valid = true
                }
                'Q' -> {
                    presentNumber = index
                    presentCommand = 'Q'
                    valid = true
                }
                'q' -> {
                    presentNumber = index
                    presentCommand = 'q'
                    valid = true
                }
                'T' -> {
                    presentNumber = index
                    presentCommand = 'T'
                    valid = true
                }
                't' -> {
                    presentNumber = index
                    presentCommand = 't'
                    valid = true
                }
                'A' -> {
                    presentNumber = index
                    presentCommand = 'A'
                    valid = true
                }
                'a' -> {
                    presentNumber = index
                    presentCommand = 'a'
                    valid = true
                }
                'Z' -> {
                    presentNumber = index
                    presentCommand = 'Z'
                    valid = true
                }
                'z' -> {
                    presentNumber = index
                    presentCommand = 'z'
                    valid = true
                }
            }

            if(valid){
                valid = false
                when(lastCommand){
                    'M' -> {
                        val temp = getCommandData(data.substring(lastNumber + 1,presentNumber))
                        for( i in 0..temp.size-2 step 2){
                            path.moveTo(temp[i+0]/divisor, temp[i+1]/divisor)//设置Path的起点
                            relativelyX = temp[i+0]/divisor
                            relativelyY = temp[i+1]/divisor
                        }
                    }
                    'm' -> {
                        val temp = getCommandData(data.substring(lastNumber + 1,presentNumber))
                        for( i in 0..temp.size-2 step 2){
                            path.rMoveTo(temp[i+0]/divisor, temp[i+1]/divisor)//设置Path的起点
                        }
                    }
                    'L' -> {
                        val temp = getCommandData(data.substring(lastNumber + 1,presentNumber))
                        for( i in 0..temp.size-2 step 2){
                            path.lineTo(temp[i+0]/divisor,temp[i+1]/divisor)
                            relativelyX = temp[i+0]/divisor
                            relativelyY = temp[i+1]/divisor
                        }


                    }
                    'l' -> {
                        val temp = getCommandData(data.substring(lastNumber + 1,presentNumber))
                        for( i in 0..temp.size-2 step 2){
                            path.rLineTo(temp[i+0]/divisor,temp[i+1]/divisor)
                        }

                    }
                    'H' -> {
                        val temp = getCommandData(data.substring(lastNumber + 1,presentNumber))
                        path.lineTo((temp[0]/divisor),relativelyY)
                        relativelyX = temp[0]/divisor
                    }
                    'h' -> {

                        val temp = getCommandData(data.substring(lastNumber + 1,presentNumber))
                        path.rLineTo((temp[0]/divisor),0f)

                    }
                    'V' -> {
                        val temp = getCommandData(data.substring(lastNumber + 1,presentNumber))
                        path.lineTo(relativelyX,temp[0]/divisor)
                        relativelyY = temp[0]/divisor
                    }
                    'v' -> {
                        val temp = getCommandData(data.substring(lastNumber + 1,presentNumber))
                        path.rLineTo(0f,(temp[0]/divisor))
                    }
                    'C' -> {
                        val temp = getCommandData(data.substring(lastNumber + 1,presentNumber))

                        for( i in 0..temp.size-6 step 6){
                            path.cubicTo(    temp[i+0]/divisor, temp[i+1]/divisor
                                ,temp[i+2]/divisor, temp[i+3]/divisor
                                ,temp[i+4]/divisor, temp[i+5]/divisor
                            )//三次贝塞尔曲线
                            relativelyX = temp[i+4]/divisor
                            relativelyY = temp[i+5]/divisor

                        }

                    }
                    'c' -> {
                        val temp = getCommandData(data.substring(lastNumber + 1,presentNumber))

                        for( i in 0..temp.size-6 step 6){
                            path.rCubicTo(   temp[i+0]/divisor, temp[i+1]/divisor
                                ,temp[i+2]/divisor, temp[i+3]/divisor
                                ,temp[i+4]/divisor, temp[i+5]/divisor
                            )
                        }

                    }
                    'S' -> {
                        val temp = getCommandData(data.substring(lastNumber + 1,presentNumber))

                        for( i in 0..temp.size-4 step 4){
                            path.quadTo(   temp[i+0]/divisor, temp[i+1]/divisor
                                ,temp[i+2]/divisor, temp[i+3]/divisor)

                            relativelyX += temp[i+2]/divisor
                            relativelyY += temp[i+3]/divisor
                        }

                    }
                    's' -> {
                        val temp = getCommandData(data.substring(lastNumber + 1,presentNumber))

                        for( i in 0..temp.size-4 step 4){
                            path.rQuadTo(   temp[i+0]/divisor, temp[i+1]/divisor
                                ,temp[i+2]/divisor, temp[i+3]/divisor )
                        }
                    }
                    'Q' -> {}
                    'q' -> {}
                    'T' -> {}
                    't' -> {}
                    'A' -> {}
                    'a' -> {
                        val temp = getCommandData(data.substring(lastNumber + 1,presentNumber))

                        for( i in 0..temp.size-7 step 7){
                            path.arcTo(   temp[i+0]/divisor, temp[i+1]/divisor
                                ,temp[i+2]/divisor, temp[i+3]/divisor
                                ,temp[i+4]/divisor, temp[i+5]/divisor,false
                            )

                        }

                    }
                    'Z' -> {}
                    'z' -> {}
                }
                lastCommand = presentCommand
                lastNumber = presentNumber

            }
        }
        path.close()
        return path
    }

    private fun getCommandData(string : String) : ArrayList<Float> {
        val temp  = ArrayList<Float>()
        val data = if(string.contains(",")) {
            string.replace("," , " ") //字串中的, 用空格替换
        }else {
            string
        }
        val stringList = data.split(" ") //分割字串到列表

        for( i in stringList){
            if (i.isNotEmpty()) temp.add(i.toFloat())
        }

        return temp
    }



}