import machine
import time
import dht
import bluetooth

# Servo moteur sur D3 (PWM manuel)
servo_pin = machine.Pin("D3", machine.Pin.OUT)

# Valeur maximale de différence de lumière
MAX_DIFF = 50000

def pwm_poor(angle):
    duty = 0.5 + (angle / 180) * 2.0  # en ms
    for _ in range(50):
        servo_pin.value(1)
        time.sleep_us(int(duty * 1000))
        servo_pin.value(0)
        time.sleep_us(int(20000 - duty * 1000))

def move_servo(angle):
    pwm_poor(angle)

# Capteurs de lumière
light_sensor_right = machine.ADC(machine.Pin("A0"))
light_sensor_left = machine.ADC(machine.Pin("A1"))

# Capteur DHT22
dht_pin = machine.Pin("D2", machine.Pin.IN, machine.Pin.PULL_UP)
dht_sensor = dht.DHT22(dht_pin)

current_angle = 90
move_servo(current_angle)

cycle_count = 0

# BLE setup
ble = bluetooth.BLE()
ble.active(True)
DEVICE_NAME = "Suiveur-Solaire"

UART_SERVICE_UUID = bluetooth.UUID("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
TX_CHAR_UUID = bluetooth.UUID("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
RX_CHAR_UUID = bluetooth.UUID("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")

UART_SERVICE = (
    UART_SERVICE_UUID,
    (
        (TX_CHAR_UUID, bluetooth.FLAG_READ | bluetooth.FLAG_NOTIFY),
        (RX_CHAR_UUID, bluetooth.FLAG_WRITE),
    ),
)

handles = ble.gatts_register_services([UART_SERVICE])
tx_handle, rx_handle = handles[0]
conn_handle = None

def on_rx(data):
    pass

def bt_callback(event, data):
    global conn_handle
    if event == 1:
        conn_handle = data[0]
        print("Appareil connecté")
    elif event == 2:
        conn_handle = None
        print("Déconnecté")
    elif event == 3 and data[1] == rx_handle:
        buffer = ble.gatts_read(rx_handle)
        on_rx(buffer)

ble.irq(bt_callback)

ADV_DATA = (
    b"\x02\x01\x06"
    + bytearray((len(DEVICE_NAME) + 1, 0x09))
    + DEVICE_NAME.encode()
)

ble.gap_advertise(100_000, ADV_DATA)
print("BLE actif :", DEVICE_NAME)

# Boucle principale
try:
    print("Suiveur solaire actif")
    while True:
        try:
            val_right = float(light_sensor_right.read_u16())
            val_left = float(light_sensor_left.read_u16())
            diff = val_left - val_right

            print(f"Lumière Gauche: {val_left}, Droite: {val_right}, Diff: {diff}")

            if diff > MAX_DIFF:
                current_angle = 0
            elif diff < -MAX_DIFF:
                current_angle = 180
            else:
                current_angle = 90 - (diff / MAX_DIFF) * 90
                current_angle = max(0, min(180, current_angle))

            move_servo(current_angle)
            print("Nouvel angle:", current_angle)

            if conn_handle is not None:
                # Message compact < 20 caractères
                lumiere_msg = f"L{int(val_left)}R{int(val_right)}A{int(current_angle)}"
                ble.gatts_write(tx_handle, lumiere_msg.encode())
                ble.gatts_notify(conn_handle, tx_handle)

        except Exception as e:
            print("Erreur capteurs lumière:", e)

        cycle_count += 1
        if cycle_count >= 5:
            cycle_count = 0
            try:
                dht_sensor.measure()
                temp = dht_sensor.temperature()
                hum = dht_sensor.humidity()
                print("Température:", temp, "C")
                print("Humidité:", hum, "%")

                if conn_handle is not None:
                    dht_msg = f"T:{temp:.1f}C H:{hum:.1f}%"
                    ble.gatts_write(tx_handle, dht_msg.encode())
                    ble.gatts_notify(conn_handle, tx_handle)

            except Exception as e:
                print("Erreur capteur DHT:", e)

        time.sleep(0.1)

except KeyboardInterrupt:
    print("Arrêt du programme")
