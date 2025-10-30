import requests
import time
import os
import socket
import winreg
from win10toast import ToastNotifier
import sys  # Importado para obter o caminho do executável

# --- Configurações fixas ---
MANAGER_IP = "10.85.0.48"
MANAGER_BASE_URL = f"http://{MANAGER_IP}:23456/api/manager"
MANAGER_AUTH_URL = f"{MANAGER_BASE_URL}/register"
MANAGER_API_URL = f"{MANAGER_BASE_URL}/heartbeat"

KEY_FILE = 'my_agent_key.txt'
HEARTBEAT_INTERVAL = 10  # Intervalo em segundos
SHUTDOWN_DELAY = 10     # 10 minutos (em segundos)
# ----------------------------

AGENT_HOSTNAME = socket.gethostname()

# Extrai grupo automaticamente do hostname
# Ex: L-10-54--01 -> grupo = L-10-54
if "--" in AGENT_HOSTNAME:
    AGENT_GROUP = AGENT_HOSTNAME.split("--")[0]
else:
    AGENT_GROUP = AGENT_HOSTNAME

# Inicializa notificações
notifier = ToastNotifier()

def exibir_mensagem_tela(msg, titulo="Aviso do Sistema"):
    """Exibe uma notificação que não bloqueia o código."""
    try:
        # threaded=True garante que o script não pause esperando a notificação
        notifier.show_toast(titulo, msg, duration=10, threaded=True)
    except Exception as e:
        print(f"[AGENTE] Falha ao exibir notificação: {e}")

def adicionar_inicio_windows():
    """Adiciona o agente para iniciar com o Windows automaticamente."""
    try:
        # Pega o caminho absoluto deste script (ex: C:\agente\agent.py)
        script_path = os.path.abspath(__file__)
        
        # Pega o caminho do executável do Python (ex: C:\Python310\python.exe)
        python_exe_path = sys.executable

        # --- CORREÇÃO PRINCIPAL ---
        # Troca 'python.exe' por 'pythonw.exe' para rodar em modo "windowless" (sem console)
        # Se 'pythonw.exe' não for encontrado, usa o 'python.exe' como fallback
        pythonw_exe_path = python_exe_path.replace("python.exe", "pythonw.exe")
        if not os.path.exists(pythonw_exe_path):
            pythonw_exe_path = python_exe_path # Fallback

        # Define o comando final que será escrito no Registro
        # Se for um script .py, usa o 'pythonw.exe' para executá-lo
        if script_path.endswith(".py"):
            # Formato: "C:\Python310\pythonw.exe" "C:\agente\agent.py"
            valor_registro = f'"{pythonw_exe_path}" "{script_path}"'
        else:
            # Se for um .exe compilado, apenas usa o caminho do .exe
            valor_registro = f'"{script_path}"'
        # --- FIM DA CORREÇÃO ---

        # Chave do Registro para apps que iniciam com o usuário
        key = winreg.HKEY_CURRENT_USER
        subkey = r"Software\Microsoft\Windows\CurrentVersion\Run"
        app_name = "AgentMonitor" # Nome da entrada no Registro

        # Abre a chave do Registro e define o valor
        with winreg.OpenKey(key, subkey, 0, winreg.KEY_SET_VALUE) as reg_key:
            winreg.SetValueEx(reg_key, app_name, 0, winreg.REG_SZ, valor_registro)

        print(f"[AGENTE] Adicionado para iniciar com o Windows: {valor_registro}")
    
    except PermissionError:
        print("[AGENTE] FALHA AO ADICIONAR AO INICIALIZAR: Permissão negada.")
        print("[AGENTE] Por favor, execute este script como Administrador pelo menos uma vez.")
    except Exception as e:
        print(f"[AGENTE] Falha desconhecida ao adicionar ao inicializar: {e}")

def register_agent():
    print(f"[AGENTE] Chave não encontrada. Tentando registrar '{AGENT_HOSTNAME}' (Grupo: {AGENT_GROUP}) no Manager em {MANAGER_IP}...")
    try:
        payload = {"hostname": AGENT_HOSTNAME, "group": AGENT_GROUP}
        # Timeout adicionado para evitar que o script trave indefinidamente
        response = requests.post(MANAGER_AUTH_URL, json=payload, timeout=10)

        if response.status_code == 201:
            data = response.json()
            key = data['key']
            agent_id = data['id']

            with open(KEY_FILE, 'w') as f:
                f.write(key)

            print(f"[AGENTE] Registro com SUCESSO. ID: {agent_id}, Chave: {key[:8]}...")
            return key
        else:
            print(f"[AGENTE] FALHA no registro (Manager respondeu {response.status_code}): {response.text}")
            return None

    except requests.exceptions.ConnectionError:
        print(f"[AGENTE] FALHA no registro. Não foi possível conectar ao Manager Auth em {MANAGER_IP}.")
        return None
    except requests.exceptions.Timeout:
        print(f"[AGENTE] FALHA no registro. Tempo de conexão esgotado (Timeout).")
        return None
    except Exception as e:
        print(f"[AGENTE] Erro inesperado no registro: {e}")
        return None

def get_or_register_key():
    if os.path.exists(KEY_FILE):
        with open(KEY_FILE, 'r') as f:
            key = f.read().strip()
            if key:
                print(f"[AGENTE] Chave carregada do arquivo: {key[:8]}...")
                return key
    
    # Se o arquivo não existe ou está vazio, registra novamente
    return register_agent()

def send_heartbeat(key):
    if not key:
        print("[AGENTE] Não é possível enviar heartbeat: Chave ausente.")
        return True # Retorna True para tentar registrar novamente no loop principal

    headers = {"Authorization": f"Bearer {key}"}
    payload = {"hostname": AGENT_HOSTNAME, "status": "online", "group": AGENT_GROUP}

    try:
        response = requests.post(MANAGER_API_URL, json=payload, headers=headers, timeout=10)

        if response.status_code == 401:
            # 401 Unauthorized - A chave é inválida
            print("[AGENTE] ERRO: O Manager rejeitou nossa chave (401).")
            if os.path.exists(KEY_FILE):
                os.remove(KEY_FILE) # Remove a chave inválida
            return True # Retorna True para forçar novo registro

        if response.status_code == 200:
            data = response.json()
            command = data.get('command')

            if command == 'shutdown':
                print("[AGENTE] Comando 'shutdown' recebido do Manager!")
                msg = f"O computador será desligado em {int(SHUTDOWN_DELAY / 60)} minutos.\nSalve seu trabalho agora."
                exibir_mensagem_tela(msg, "Desligamento Remoto")
                
                # O comando 'shutdown' usa segundos
                os.system(f'shutdown /s /t {SHUTDOWN_DELAY} /c \"Desligamento remoto iniciado pelo Manager. Salve seu trabalho.\"')
                
                # Se vai desligar, podemos parar o loop
                return False # Retorna False para sair do loop while

            elif command == 'ok':
                print(f"[AGENTE] ({AGENT_HOSTNAME} / {AGENT_GROUP}) Heartbeat enviado. Resposta: OK.")
                return True # Continua rodando
            
            else:
                print(f"[AGENTE] Resposta desconhecida do Manager: {data}")
                return True

        else:
            print(f"[AGENTE] Erro inesperado do Manager (Status {response.status_code}): {response.text}")
            return True

    except requests.exceptions.ConnectionError:
        print(f"[AGENTE] FALHA no heartbeat. Não foi possível conectar ao Manager API em {MANAGER_IP}.")
        return True
    except requests.exceptions.Timeout:
        print(f"[AGENTE] FALHA no heartbeat. Tempo de conexão esgotado (Timeout).")
        return True
    except Exception as e:
        print(f"[AGENTE] Erro inesperado no heartbeat: {e}")
        return True

if __name__ == '__main__':
    print(f"--- Iniciando Agente na máquina: {AGENT_HOSTNAME} ---")
    print(f"--- Grupo de Agente: {AGENT_GROUP} ---")
    print(f"--- Conectando ao Manager em: {MANAGER_IP}:23456 ---")

    # Adiciona o agente ao inicializar do Windows
    # Isso pode falhar se não for executado como Admin na primeira vez
    adicionar_inicio_windows()

    # Obtém ou registra a chave do agente
    current_key = get_or_register_key()

    # Loop principal de heartbeat
    while True:
        if not current_key:
            print("[AGENTE] Tentando obter a chave novamente...")
            current_key = get_or_register_key()
            if not current_key:
                print(f"[AGENTE] Falha ao obter a chave, tentando em {HEARTBEAT_INTERVAL} segundos...")
                time.sleep(HEARTBEAT_INTERVAL)
                continue # Pula para a próxima iteração do loop
        
        # Envia o heartbeat
        keep_running = send_heartbeat(current_key)

        if not keep_running:
            print("[AGENTE] Comando de parada recebido. Encerrando o agente.")
            break # Sai do loop while

        # Se a chave foi rejeitada (401), send_heartbeat terá removido o arquivo
        # e o 'current_key' precisa ser limpo para forçar o registro
        if not os.path.exists(KEY_FILE):
            print("[AGENTE] Chave local apagada (provavelmente 401). Forçando novo registro na próxima iteração.")
            current_key = None # Força a re-obtenção da chave

        print(f"[AGENTE] Próximo heartbeat em {HEARTBEAT_INTERVAL} segundos...")
        time.sleep(HEARTBEAT_INTERVAL)
