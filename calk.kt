package classis

import kotlin.math.abs

const val isPrint = true  //выводить промежуточные вычисления
const val eps = 1e-10 // точность округления

interface Expression

class Number(val value: Double) : Expression {
    override fun toString(): String {
        return if (abs(value.minus(value.toLong())) < eps) "${value.toLong()}" else "$value"
    }
}

class Action(val value: String) : Expression {
    fun getPriority(str: String): Int =
        when (str[0]) {
            '+', '-' -> 1
            '*', '/' -> 2
            '(', ')' -> 0
            else -> throw Throwable("неизвестная операция")
        }

    operator fun compareTo(item: Expression): Int {
        if (item is Action) return getPriority(item.toString()) - getPriority(this.toString())
        else throw Throwable("сравнение невозможно")
    }

    fun count(b: Number, a: Number): Number {
        val d = when (this.value[0]) {
            '+' -> a.value + b.value
            '-' -> a.value - b.value
            '*' -> a.value * b.value
            '/' -> a.value / b.value
            else -> throw Throwable("неизвестная операция")
        }
        return Number(d)
    }

    override fun toString()= value
}

//парсинг строки
fun parsing(str: String): MutableList<Expression> {
    val input = mutableListOf<Expression>()

    val regexpNumber =
        """[0-9.]{1,10}|[+\-*/()]"""  // выделение подстрок, чисел с точкой или отдельно знака арифметических действий и скобок
    val patternNumber = regexpNumber.toRegex()
    val foundNumber = patternNumber.findAll(str)

    var i = 0
    for (found in foundNumber) {
        if (found.value[0] in '0'..'9') {  // если символ числовой, то интерпритируем
            input.add(Number(found.value.toDouble()))
        } else { // проверяем сразу на скобки
            if (found.value[0] == '(') {
                i++
            }
            if (found.value[0] == ')') {
                i--
                if (i < 0) throw Throwable("скобка закрывается раньше, чем открывается")
            }
            input.add(Action(found.value))
        }
    }
    if (i != 0) throw Throwable("скобки не парные")

    // обработка унарного минуса
    var index = 0;
    while (index < input.size) {
        val iter = input[index]
        if (iter is Action)
            if (iter.value == "-") {
                if ((index == 0) || ((index > 0) && (input[index - 1] is Action) && ((input[index - 1] as Action).value == "(")))
                    if (input[index + 1] is Number) {
                        val value = (-1.0) * (input[index + 1] as Number).value
                        input.removeAt(index + 1)
                        input.removeAt(index)
                        input.add(index, Number(value))
                        if (isPrint) println("Унарный ${iter} [${index}]=$value")
                    }
            }
        index++
    }

    return input
}

//алгоритм сортировочной станции (перевод в обратную польскую запись)
fun polishNotation(input: MutableList<Expression>): MutableList<Expression> {
    if (isPrint) println("Перевод в обратную польскую запись:")
    val stack = mutableListOf<Expression>()
    val output = mutableListOf<Expression>()

    for (item in input) {
        if (isPrint) print("$item   \t:")
        when (item) {
            is Number -> output.add(0, item)
            is Action -> {
                when (item.value) {
                    "(" -> stack.add(0, item)
                    ")" -> {
                        while (stack[0].toString()[0] != '(') {
                            output.add(0, stack.removeAt(0))
                        }
                        stack.removeAt(0) // удаляем скобку
                    }
                    else -> {
                        if (stack.isNotEmpty())
                            if (stack[0] as Action <= item)
                                output.add(0, stack.removeAt(0))
                        stack.add(0, item)
                    }
                }
            }
        }
        if (isPrint) println("Stack:" + stack + "\tOutput:" + output) //вывод полученной строки
    }
    while (stack.isNotEmpty()) {
        output.add(0, stack.removeAt(0))
    }
    return output
}

//вычисление через стек
fun calculateOnStack(output: MutableList<Expression>): Expression {
    val stack = mutableListOf<Expression>()
    if (isPrint) println("Вычисление:")
    do {
        var item: Expression = output.removeLast()
        when (item) {
            is Number -> stack.add(0, item)
            is Action -> {
                if (stack.size > 1) stack.add(0, item.count(stack.removeAt(0) as Number, stack.removeAt(0) as Number))
                else throw Throwable("не хватает аргументов для бинарной операции")
            }
        }
        if (isPrint) println(
            item.toString() + "   \tStack:" + stack.joinToString(
                " ",
                "",
                ""
            )
        ) //вывод полученной строки
    } while (output.isNotEmpty())
    return stack[0]
}

//Рекурсивный подсчет через выделение подстрок без скобок
fun subListRecursion(input: MutableList<Expression>, fromIndex: Int) {
    var index = fromIndex
    var isClose = false
    while (!isClose && (index < input.size)) {
        if (input[index] is Action) {
            if ((input[index] as Action).value == "(") {
                subListRecursion(input, index + 1)
            } else if ((input[index] as Action).value == ")") {
                isClose = true
                val tmp = input.subList(fromIndex, index).toMutableList()

                for (iter in 0..index - fromIndex + 1) {
                    input.removeAt(fromIndex - 1)
                }
                input.add(fromIndex - 1, CalculateSubList(tmp))
            }
        }
        index++
    }
}

//подсчет простого выражения без скобок
fun CalculateSubList(input: MutableList<Expression>): Expression {
    subListRecursion(input, 0)
    if (isPrint) print("\nSubList: " + input.joinToString(" ", "", ""))

    for (priority in 2 downTo 1) {
        var index: Int = 1

        while (index < input.size) {
            val iter = input[index]
            if (iter is Action) {
                if (iter.getPriority(iter.value) == priority) {
                    val e = iter.count(input[index + 1] as Number, input[index - 1] as Number)
                    input.removeAt(index + 1)
                    input.removeAt(index)
                    input.removeAt(index - 1)
                    input.add(index - 1, e)
                    if (isPrint) print(" = " + input.joinToString(" ", "", ""))
                    index -= 2
                }
            }
            index++
        }

    }
    return input[0]
}

fun main() {

    //входная строка
    val str = "-1*(6.5*10-4.5-0.5)/(1+1*2)+1"
    //"-1*(-3.7-46.3)*((62.5/2)-1.25)"  // =1500
    //"(-2+2*2)-2/2" //=1
    //"(-1+1+(1+1-1)+1+(-1)+1+1)+1" //=4
    //"-1*(6.5*10-4.5-0.5)/(1+1*2)+1" //=19
    //"1/3+1/5+1/7"  // = 0.6761904761904762

    println("Входная строка: $str")

    val input = parsing(str)
    println("\nРазобранная строка:\n" + input.joinToString(prefix = "",postfix = "",separator = " "))
    //вывод полученной строки

    val output = polishNotation(input)
    println("Обратная польская запись:\n" + output.joinToString(" ", "", ""))
    //вывод полученной строки

    val answer = calculateOnStack(output)
    println("Результат:\n" + input.joinToString(" ", "", "")+"="+answer)
    // вывод результата

    print("\nРекурсия")
    println("\nРезультат: \n${input.joinToString("", "", "")}=${CalculateSubList(input.toMutableList())}")

}


