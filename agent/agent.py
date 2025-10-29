import requests
import time
import os
import sys
import socket
import argparse

# --- Parser de Argumentos ---
parser = argparse.ArgumentParser(description="Agente de Monitoramento")
parser.add_argument(
    'group', 
    nargs='?', 
    default='default', 
    help='O grupo do agente (ex: "L-10-54")'
)
parser.add_argument(
    '--ip', 
    default='127.0.0.1', 
    help='O endereço IP do Manager (padrão: 127.0.0.1)'
)
args = parser.parse_args()
# ------------------------------

AGENT_HOSTNAME = socket.gethostname() 
AGENT_GROUP = args.group
MANAGER_IP = args.ip

MANAGER_BASE_URL = f"http://{MANAGER_IP}:23456/api/manager"
MANAGER_AUTH_URL = f"{MANAGER_BASE_URL}/register" 
MANAGER_API_URL = f"{MANAGER_BASE_URL}/heartbeat" 
KEY_FILE = 'my_agent_key.txt'
HEARTBEAT_INTERVAL = 240

AGENT_KEY = None

def register_agent():
    print(f"[AGENTE] Chave não encontrada. Tentando registrar '{AGENT_HOSTNAME}' (Grupo: {AGENT_GROUP}) no Manager em {MANAGER_IP}...")
    try:
        payload = {"hostname": AGENT_HOSTNAME, "group": AGENT_GROUP}
        response = requests.post(MANAGER_AUTH_URL, json=payload)
        
        if response.status_code == 201:
            data = response.json()
            key = data['key']
            agent_id = data['id']
            
            with open(KEY_FILE, 'w') as f:
                f.write(key)
                
            print(f"[AGENTE] Registro com SUCESSO. ID: {agent_id}, Chave: {key[:8]}...")
            return key
        else:
            print(f"[AGENTE] FALHA no registro: {response.text}")
            return None
            
    except requests.exceptions.ConnectionError:
        print(f"[AGENTE] FALHA no registro. Não foi possível conectar ao Manager Auth em {MANAGER_IP}.")
        return None

def get_or_register_key():
    if os.path.exists(KEY_FILE):
        with open(KEY_FILE, 'r') as f:
            key = f.read().strip()
            print(f"[AGENTE] Chave carregada do arquivo: {key[:8]}...")
            return key
    else:
        return register_agent()

def send_heartbeat(key):
    if not key:
        print("[AGENTE] Não é possível enviar heartbeat: Chave ausente.")
        return True 

    headers = {"Authorization": f"Bearer {key}"}
    
    payload = {"hostname": AGENT_HOSTNAME, "status": "online", "group": AGENT_GROUP}
    
    try:
        response = requests.post(MANAGER_API_URL, json=payload, headers=headers)
        
        if response.status_code == 401:
            print("[AGENTE] ERRO: O Manager rejeitou nossa chave (401).")
            print("[AGENTE] Apagando chave local para tentar novo registro.")
            os.remove(KEY_FILE)
            return True 

        if response.status_code == 200:
            data = response.json()
            command = data.get('command')
            
            if command == 'shutdown':
                print("[AGENTE] Comando 'shutdown' recebido do Manager!")
                print("[AGENTE] INICIANDO DESLIGAMENTO REAL DA MÁQUINA EM 1 SEGUNDO...")
                os.system('shutdown /s /t 1')
                return False 
            
            elif command == 'ok':
                print(f"[AGENTE] ({AGENT_HOSTNAME} / {AGENT_GROUP}) Heartbeat enviado. Resposta: OK.")
                return True 

        else:
            print(f"[AGENTE] Erro inesperado do Manager: {response.status_code}")
            return True 
            
    except requests.exceptions.ConnectionError:
        print(f"[AGENTE] FALHA no heartbeat. Não foi possível conectar ao Manager API em {MANAGER_IP}.")
        return True 

if __name__ == '__main__':
    print(f"--- Iniciando Agente na máquina: {AGENT_HOSTNAME} ---")
    print(f"--- Grupo de Agente: {AGENT_GROUP} ---")
    print(f"--- Conectando ao Manager em: {MANAGER_IP}:23456 ---")
    current_key = get_or_register_key()
    
    while True:
        keep_running = send_heartbeat(current_key)
        
        if not keep_running:
            break
            
        if not current_key:
            print("[AGENTE] Tentando obter a chave novamente...")
            current_key = get_or_register_key()
            if not current_key:
                print(f"[AGENTE] Falha ao obter a chave, tentando em {HEARTBEAT_INTERVAL} segundos...")
                
        print(f"[AGENTE] Próximo heartbeat em {HEARTBEAT_INTERVAL} segundos...")
        time.sleep(HEARTBEAT_INTERVAL)
        
    print("[AGENTE] Processo finalizado.")