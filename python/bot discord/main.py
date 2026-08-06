import time

num1 = input("Введите первое число: ")
oper = input("Введите операцыю: ")
num2 = input("Введите второе число: ")
otv = ""

try:
    num1 = int(num1)
    num2 = int(num2)
except ValueError:
    print("Ошибка: введите числа")
    exit()
else:

    if oper == "+":
        otv = num1+num2
    if oper == "-":
        otv = num1-num2
    if oper == "*":
        otv = num1*num2
    if oper == "/":
        try:
            otv = num1/num2
        except ZeroDivisionError:
            print("Ошибка: деление на ноль")
            exit()
    print("Ответ: ", otv)
