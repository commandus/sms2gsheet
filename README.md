# sberbanksms

```
./sbrf900cli -f /dev/ttyUSB0
```

## Установка

### Linux

Вставьте USB модем в компьютер под управлением Linux.

Если будет предложено установить Connect Manager, отмените (нажмите Cancel).

Не устанавливайте Connect Manager!

Проверьте, установился ли COM порт устройства:

```
dmesg
[    6.800869] usbcore: registered new interface driver usbserial
[    6.800887] usbcore: registered new interface driver usbserial_generic
[    6.800899] usbserial: USB Serial support registered for generic
[    6.808022] usbcore: registered new interface driver option
[    6.808033] usbserial: USB Serial support registered for GSM modem (1-port)
[    6.808077] option 3-1:1.0: GSM modem (1-port) converter detected
[    6.808226] usb 3-1: GSM modem (1-port) converter now attached to ttyUSB0
[    6.808258] option 3-1:1.2: GSM modem (1-port) converter detected
[    6.808306] usb 3-1: GSM modem (1-port) converter now attached to ttyUSB1
[    6.808319] option 3-1:1.3: GSM modem (1-port) converter detected
[    6.808351] usb 3-1: GSM modem (1-port) converter now attached to ttyUSB2
[    6.827933] usbcore: registered new interface driver cdc_ncm
[    6.831735] usbcore: registered new interface driver cdc_wdm

```

Проверьте путь COM порт устройства:

```
ls -la /dev/ttyUSB*
crw-rw---- 1 root dialout 188, 0 Oct 12 10:03 /dev/ttyUSB0
crw-rw---- 1 root dialout 188, 1 Oct 12 10:02 /dev/ttyUSB1
crw-rw---- 1 root dialout 188, 2 Oct 12 10:02 /dev/ttyUSB2
```

#### Ошибка Permission denied


- Добавьте пользователя в группу устройства (dialout)
```
ls -la /dev/ttyUSB0
crw-rw---- 1 root dialout 188, 0 Oct 12 09:53 /dev/ttyUSB0
sudo usermod -a -G dialout $USER
sudo reboot
```
- или запускайте приложение от имени root (не рекомендуется)

Sberbank's client to transfer money using SMS gate.


- [USSD команды моб. клиента](https://docs.google.com/document/d/18z0JOxaSGA1zThvz8CO3H_L9TrqxN-e0eAevfTaf-wU/edit)

SMS

Запрос

ПЕРЕВОД 1234567891234567 5000
1234567891234567 – полный номер карты получателя – клиента Сбербанка. 5000 – сумма перевода в валюте счета «карты отправителя».
ПЕРЕВОД  9xx1234567 5000

Вместо ПЕРЕВОД в SMS-сообщении может быть указано PEREVOD, ПЕРЕВЕСТИ, PEREVESTI.
1234567891234567 – полный номер карты получателя – клиента Сбербанка. 5000 – сумма перевода в валюте счета «карты отправителя».

Ответ

Для перевода 5000р с карты VISA1234 на карту получателя ИВАН ИВАНОВИЧ И. отправьте код 12388 на номер 900

Ответ

Для перевода 500р с карты VISA1234 на карту получателя ИВАН ИВАНОВИЧ И. отправьте код 14488 на номер 900
