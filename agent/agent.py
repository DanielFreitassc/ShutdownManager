import requests
import time
import os
import socket
import winreg
from win10toast import ToastNotifier
import sys
import uuid  # ✅ para gerar chave única

# --- Configurações fixas ---
MANAGER_IP = "10.85.0.48"
MANAGER_BASE_URL = f"http://{MANAGER_IP}:23456/api/manager"
MANAGER_AUTH_URL = f"{MANAGER_BASE_URL}/register"
MANAGER_API_URL = f"{MANAGER_BASE_URL}/heartbeat"

HEARTBEAT_INTERVAL = 10
SHUTDOWN_DELAY = 1 * 60  # 10 minutos
REG_PATH = r"SOFTWARE\AgentMonitor"
REG_VALUE_NAME = "AgentKey"
# ----------------------------

AGENT_HOSTNAME = socket.gethostname()
AGENT_GROUP = AGENT_HOSTNAME.split("--")[0] if "--" in AGENT_HOSTNAME else AGENT_HOSTNAME

notifier = ToastNotifier()


def exibir_mensagem_tela(msg, titulo="Aviso do Sistema"):
    try:
        notifier.show_toast(titulo, msg, duration=10, threaded=True)
    except Exception as e:
        print(f"[AGENTE] Falha ao exibir notificação: {e}")


def salvar_chave_global(key: str):
    """Salva a chave no Registro do Windows (HKEY_LOCAL_MACHINE)"""
    try:
        with winreg.CreateKey(winreg.HKEY_LOCAL_MACHINE, REG_PATH) as reg_key:
            winreg.SetValueEx(reg_key, REG_VALUE_NAME, 0, winreg.REG_SZ, key)
        print(f"[AGENTE] Chave salva no registro global (HKLM\\{REG_PATH})")
    except PermissionError:
        print("[AGENTE] ERRO: Permissão negada ao gravar no registro global.")
        print("         Execute este script como administrador pelo menos uma vez.")
    except Exception as e:
        print(f"[AGENTE] Falha ao salvar chave no registro global: {e}")


def carregar_chave_global() -> str | None:
    """Lê a chave global do Registro"""
    try:
        with winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, REG_PATH) as reg_key:
            key, _ = winreg.QueryValueEx(reg_key, REG_VALUE_NAME)
            print(f"[AGENTE] Chave carregada do registro global: {key[:8]}...")
            return key
    except FileNotFoundError:
        return None
    except PermissionError:
        print("[AGENTE] ERRO: Sem permissão para ler o registro global.")
        return None
    except Exception as e:
        print(f"[AGENTE] Erro ao ler chave do registro global: {e}")
        return None


def gerar_chave_local() -> str:
    """Gera uma nova chave única localmente"""
    key = str(uuid.uuid4())
    print(f"[AGENTE] Nova chave gerada localmente: {key[:8]}...")
    salvar_chave_global(key)
    return key


def register_agent(key: str):
    """Envia os dados + chave gerada para o Manager"""
    print(f"[AGENTE] Tentando registrar '{AGENT_HOSTNAME}' (Grupo: {AGENT_GROUP}) no Manager em {MANAGER_IP}...")
    try:
        payload = {
            "hostname": AGENT_HOSTNAME,
            "group": AGENT_GROUP,
            "agentKey": key  # ✅ Envia chave gerada localmente
        }
        response = requests.post(MANAGER_AUTH_URL, json=payload, timeout=10)

        if response.status_code in (200, 201):
            data = response.json()
            print(f"[AGENTE] Registro aprovado pelo Manager. ID: {data.get('id')}, status: {data.get('status', 'ok')}")
            return True
        elif response.status_code == 403:
            print("[AGENTE] Registro rejeitado pelo Manager (aguardando aprovação).")
            return False
        else:
            print(f"[AGENTE] FALHA no registro ({response.status_code}): {response.text}")
            return False

    except requests.exceptions.ConnectionError:
        print(f"[AGENTE] FALHA no registro. Não foi possível conectar ao Manager Auth em {MANAGER_IP}.")
        return False
    except Exception as e:
        print(f"[AGENTE] Erro inesperado no registro: {e}")
        return False


def get_or_register_key():
    """Obtém a chave do registro ou gera uma nova"""
    key = carregar_chave_global()
    if not key:
        key = gerar_chave_local()
        register_agent(key)
    else:
        print("[AGENTE] Chave existente detectada, validando com Manager...")
        register_agent(key)
    return key


def send_heartbeat(key):
    if not key:
        print("[AGENTE] Não é possível enviar heartbeat: Chave ausente.")
        return True

    headers = {"Authorization": f"Bearer {key}"}
    payload = {"hostname": AGENT_HOSTNAME, "status": "online", "group": AGENT_GROUP}

    try:
        response = requests.post(MANAGER_API_URL, json=payload, headers=headers, timeout=10)
        if response.status_code == 401:
            print("[AGENTE] ERRO: O Manager rejeitou a chave (401). Limpando registro.")
            try:
                with winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, REG_PATH, 0, winreg.KEY_SET_VALUE) as reg_key:
                    winreg.DeleteValue(reg_key, REG_VALUE_NAME)
            except Exception as e:
                print(f"[AGENTE] Falha ao remover chave: {e}")
            return True

        if response.status_code == 200:
            data = response.json()
            command = data.get('command')
            if command == 'shutdown':
                print("[AGENTE] Comando 'shutdown' recebido!")
                msg = f"O computador será desligado em {int(SHUTDOWN_DELAY / 60)} minutos."
                exibir_mensagem_tela(msg, "Desligamento Remoto")
                os.system(f'shutdown /s /t {SHUTDOWN_DELAY} /c "Desligamento remoto iniciado pelo Manager."')
                return False
            elif command == 'ok':
                print(f"[AGENTE] ({AGENT_HOSTNAME}) Heartbeat OK.")
                return True
            else:
                print(f"[AGENTE] Resposta desconhecida: {data}")
                return True
        else:
            print(f"[AGENTE] Erro inesperado ({response.status_code}): {response.text}")
            return True
    except Exception as e:
        print(f"[AGENTE] Erro no heartbeat: {e}")
        return True


if __name__ == '__main__':
    print(f"--- Iniciando Agente: {AGENT_HOSTNAME} ---")
    print(f"--- Grupo: {AGENT_GROUP} ---")
    print(f"--- Manager: {MANAGER_IP}:23456 ---")

    current_key = get_or_register_key()

    while True:
        if not current_key:
            print("[AGENTE] Tentando obter chave novamente...")
            current_key = get_or_register_key()
            time.sleep(HEARTBEAT_INTERVAL)
            continue

        keep_running = send_heartbeat(current_key)
        if not keep_running:
            print("[AGENTE] Encerrando agente.")
            break

        time.sleep(HEARTBEAT_INTERVAL)
