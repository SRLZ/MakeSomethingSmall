import serial
import time
ser = serial.Serial("/dev/ttyAMA0", 115200)  # 位置1
#ser.flushInput()  # 位置2
#res=ser.write("*123.456#06.333#023.456#013.456#180.456#323.456#103.446#001.456#@".encode("utf-8"))  # 位置3
#print(res)
def main():
    while False:
        count = ser.inWaiting()  # 位置4
        if count != 0:
            recv = ser.read(count)  # 位置5
            ser.write("Recv some data is : ".encode("utf-8"))  # 位置6
            ser.write(recv)  # 位置7
            ser.flushInput()
        time.sleep(0.1)  # 位置8
    res=ser.write("*123.456#06.333#023.456#013.456#180.456#323.456#103.446#001.456#@".encode("utf-8"))
    print(res)
    time.sleep(2)
if __name__ == '__main__':
    main()
