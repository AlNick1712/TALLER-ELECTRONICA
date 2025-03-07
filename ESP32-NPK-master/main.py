import machine
import time
import network
import usocket as socket
import time
import json
import urequests as request

#Start wifi AP

def ap_start():
    AP_name = "SENSOR NPK"
    AP_passw = "12345678"

    ap = network.WLAN(network.WLAN.IF_AP)
    ap.config(essid=AP_name, password=AP_passw)
    ap.config(max_clients=10)
    ap.active(True)

    print('Connection successful')
    print(ap.ifconfig())

    return ap

#Define loop for await wifi connection

def stupid_loop():

    x = 1

    net = ap_start()

    while True:
        x += 1
        print(x)
        time.sleep(0.2)
        if net.isconnected():
            print(net.status())
            return net
            break
        else:
            ap_start()

# Define UART pins
senssor = machine.UART(1, baudrate=19200, tx=machine.Pin(17), rx=machine.Pin(16))

#machine.Pin(17)
#machine.Pin(16)

#machine.Pin(41)
#machine.Pin(40)

def send_modbus_query(query):
    senssor.write(bytes(query))

# Define the fixed Modbus query
read_registers_query = bytearray([0x01, 0x03, 0x00, 0x00, 0x00, 0x08, 0x44, 0x0C])
change_baud_query = bytearray([])

# Fetch data

def fetch_senssor(query):
    
    send_modbus_query(query)
    time.sleep(0.5)
    
    response = senssor.read()
    
    print('Raw response: ', response)

    # Parse response if no data is send from sensor
    if response == None:
        response = {'NO DATA' : 'NO DATA'}
        return response
    else:
        # Parse response from sensor

        celcius_value = response[3] << 8 | response[4]
        humidity_value = response[5] << 8 | response[6]
        conduc_value = response[7] << 8 | response[8]
        #salty_value = response[9] << 8 | response[10]
        #pH_value = response[11] << 8 | response[12]
        n_value = response[13] << 8 | response[14]
        p_value = response[15] << 8 | response[16]
        k_value = response[17] << 8 | response[18]
        print("Temperature value:", (celcius_value)/10, 'C')
        print("Humidity value:", (humidity_value)/10, '%')
        print("Conductivity value:", conduc_value, 'us/cm')
        #print("Salinity value:", salty_value, 'mg/L')
        #print("pH value:", (pH_value)/10)
        print("(N) Nitrogen value:", n_value, 'mg/kg')
        print("(P) Phosporus value:", p_value, 'mg/kg')
        print("(K) Potasiunm value:", k_value, 'mg/kg')
        
        #create json-type document for store sensor data

        json_senssor = {
            "Temperatura_valor" : celcius_value,
            "Humedad_value" : humidity_value,
            "conduc_value" : conduc_value,
            "nitrogeno_value" : n_value,
            "Fosforo_value" : p_value,
            "Potasio_value" : k_value,
            }
        
        json_html = json.dumps(json_senssor)
        
        time.sleep(1)
    
        return json_html

#setup local host

def socket_setup():
    
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    
    s.bind(('', 5000))
    
    s.listen(5)             #number of concurrent clients
    
    return s

def send_html(s, json_html):
    
    #accept incoming requests

    (clientsocket, address) = s.accept()
    
    print("Connected to: ", address)
    
    req = clientsocket.recv(1024)
    
    #HTTP header

    clientsocket.send('HTTP/1.1 200 OK\n')
    clientsocket.send('Content-Type: text/html\n')
    clientsocket.send('Connection: close\n\n')

    #send sensor json to client socket

    print('json_payload  ', json_html)
    payload_bytes = json.dumps(json_html)
    p = payload_bytes.encode('utf-8')
    print('payload_bytes  ', payload_bytes)
    clientsocket.send(payload_bytes)
    
    clientsocket.sendall('\nEND')
    
    clientsocket.close()
    
    #payload_json = json.dumps(payload)  # Convert dict to JSON string
    #payload_bytes = json_html.encode()

    #print('bytes: ', payload_bytes)

    #try:
        # For MicroPython, use urequests instead of requests
        #r = requests.post('POST', "http://localhost:5000", json=payload_bytes)
        #r.raise_for_status()  # Raise an HTTPError for bad responses
        #print(r.json())
        #clientsocket.close()
    #except Exception as e:
        #print(f"Error sending data: {e}")
        
def main():
    
    while True:
        
        net = stupid_loop()
        
        x = socket_setup()
        
        while True:
            if (net.isconnected()):
                
                json_html = fetch_senssor(read_registers_query)
                
                print(net.isconnected(), net.status('stations'), net.status())
                
                send_html(x, json_html)
                
                print("Sent")
                
                time.sleep(2)
            else:
                break
            
main()