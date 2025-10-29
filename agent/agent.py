import requests
import time
import os
import sys
import socket

# --- Configuração do Agente ---
AGENT_HOSTNAME = socket.gethostname() 

# *** NOVO: Lê o grupo da linha de comando ***
# Ex: python agent.py "Grupo-Servidores-Web"
if len(sys.argv) > 1:
    AGENT_GROUP = sys.argv[1] # Pega o primeiro argumento
else:
    AGENT_GROUP = "default" # Grupo padrão se nada for passado
# *****************************************

# APONTA PARA O SPRING BOOT
MANAGER_AUTH_URL = "http://127.0.0.1:8080/api/manager/register" 
MANAGER_API_URL = "http://127.0.0.1:8080/api/manager/heartbeat" 
KEY_FILE = 'my_agent_key.txt'
HEARTBEAT_INTERVAL = 240
# ------------------------------

AGENT_KEY = None

def register_agent():
    """
    Entra em contato com o 'Cartório' (Auth) para se registrar.
    """
    print(f"[AGENTE] Chave não encontrada. Tentando registrar '{AGENT_HOSTNAME}' (Grupo: {AGENT_GROUP}) no Manager Auth...")
    try:
        # *** ALTERADO: Envia o hostname E o grupo ***
        payload = {"hostname": AGENT_HOSTNAME, "group": AGENT_GROUP}
        # *****************************************
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
        print("[AGENTE] FALHA no registro. Não foi possível conectar ao Manager Auth.")
        return None

def get_or_register_key():
    """
    Verifica se a chave já existe localmente.
    Se não, chama a função de registro.
    """
    if os.path.exists(KEY_FILE):
        with open(KEY_FILE, 'r') as f:
            key = f.read().strip()
            print(f"[AGENTE] Chave carregada do arquivo: {key[:8]}...")
            return key
    else:
        return register_agent()

def send_heartbeat(key):
    """
    Envia um 'ping' para o Manager API e escuta a resposta.
    """
    if not key:
        print("[AGENTE] Não é possível enviar heartbeat: Chave ausente.")
        return True 

    headers = {"Authorization": f"Bearer {key}"}
    
    # *** ALTERADO: Envia o grupo também no heartbeat ***
    # (Isso permite que o agente mude de grupo dinamicamente se o admin alterar)
    payload = {"hostname": AGENT_HOSTNAME, "status": "online", "group": AGENT_GROUP}
    # **************************************************
    
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
                print("[AGENTE] Simulando desligamento...")
                return False 
            
            elif command == 'ok':
                print(f"[AGENTE] ({AGENT_HOSTNAME} / {AGENT_GROUP}) Heartbeat enviado. Resposta: OK.")
                return True 

        else:
            print(f"[AGENTE] Erro inesperado do Manager: {response.status_code}")
            return True 
            
    except requests.exceptions.ConnectionError:
        print("[AGENTE] FALHA no heartbeat. Não foi possível conectar ao Manager API.")
        return True 

# --- Loop Principal do Agente ---
if __name__ == '__main__':
    print(f"--- Iniciando Agente na máquina: {AGENT_HOSTNAME} ---")
    print(f"--- Grupo de Agente: {AGENT_GROUP} ---")
    current_key = get_or_register_key()
    
    while True:
        keep_running = send_heartbeat(current_key)
        
        if not keep_running:
            break
            
        if not current_key:
            print("[AGENTE] Tentando obter a chave novamente...")
            current_key = get_or_register_key()
            if not current_key:
                print("[AGENTE] Falha ao obter a chave, tentando em 4 minutos...")
                
        print(f"[AGENTE] Próximo heartbeat em {HEARTBEAT_INTERVAL} segundos...")
        time.sleep(HEARTBEAT_INTERVAL)
        
    print("[AGENTE] Processo finalizado.")