"use client"

import { useState, useEffect } from "react"
import { api } from "@/lib/api"
import type { Agent, PaginatedResponse } from "@/types"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { toast } from "react-toastify"
import { Loader2, ChevronLeft, ChevronRight, Server, Power, PowerOff, RefreshCw } from "lucide-react"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { AgentStats } from "@/components/agent-stats"

export default function AgentsPage() {
  const [agents, setAgents] = useState<Agent[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [shutdownDialogOpen, setShutdownDialogOpen] = useState(false)
  const [shutdownType, setShutdownType] = useState<"single" | "group" | "all">("single")
  const [selectedAgent, setSelectedAgent] = useState<Agent | null>(null)
  const [selectedGroup, setSelectedGroup] = useState("")
  const [shutdownCommand, setShutdownCommand] = useState("shutdown")
  const [scheduledTime, setScheduledTime] = useState("") 
  const [scheduleChecked, setScheduleChecked] = useState(false) // checkbox agendar
  const [executing, setExecuting] = useState(false)

  const pageSize = 10

  const fetchAgents = async (page: number) => {
    setLoading(true)
    try {
      const response = await api.get<PaginatedResponse<Agent>>(
        `/api/manager/admin/agents?page=${page}&size=${pageSize}`,
      )
      setAgents(response.data.content)
      setTotalPages(response.data.totalPages)
      setTotalElements(response.data.totalElements)
    } catch (error: any) {
      console.error("[v0] Error fetching agents:", error)
      toast.error(error.response?.data?.message || "Não foi possível carregar as máquinas.")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchAgents(page)
  }, [page])

  const stats = {
    total: totalElements,
    online: agents.filter((a) => a.status === "online").length,
    offline: agents.filter((a) => a.status === "offline").length,
    new: agents.filter((a) => a.status === "new").length,
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "online":
        return <Badge className="bg-green-500">Online</Badge>
      case "offline":
        return <Badge variant="destructive">Offline</Badge>
      case "new":
        return <Badge variant="secondary">Novo</Badge>
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString)
      return date.toLocaleString("pt-BR")
    } catch {
      return dateString
    }
  }

  const uniqueGroups = Array.from(new Set(agents.map((agent) => agent.group))).sort()

  const openShutdownDialog = (type: "single" | "group" | "all", agent?: Agent) => {
    setShutdownType(type)
    setSelectedAgent(agent || null)
    setShutdownDialogOpen(true)
    setScheduledTime("")
    setScheduleChecked(false)
  }

  const executeShutdown = async () => {
    if ((shutdownType === "group" && !selectedGroup) || !shutdownCommand) return

    setExecuting(true)
    try {
      if (scheduleChecked && scheduledTime) {
        // usar o endpoint novo de agendamento
        const payload: any = {
          scheduledFor: scheduledTime,
          allHostsCommand: shutdownType === "all" ? { command: shutdownCommand } : null,
          groupCommand: shutdownType === "group" ? { group: selectedGroup, command: shutdownCommand } : null,
          hostCommand: shutdownType === "single" && selectedAgent ? { hostname: selectedAgent.hostname, command: shutdownCommand } : null
        }
        await api.post("/api/manager/admin/schedule_command", payload)
        toast.success(`Comando de ${shutdownCommand} agendado com sucesso!`)
      } else {
        // enviar imediatamente usando os endpoints antigos
        if (shutdownType === "single" && selectedAgent) {
          await api.post("/api/manager/admin/queue_command", { hostname: selectedAgent.hostname, command: shutdownCommand })
        } else if (shutdownType === "group" && selectedGroup) {
          await api.post("/api/manager/admin/queue_command_group", { group: selectedGroup, command: shutdownCommand })
        } else if (shutdownType === "all") {
          await api.post("/api/manager/admin/queue_command_all", { command: shutdownCommand })
        }
        toast.success(`Comando de ${shutdownCommand} enviado com sucesso!`)
      }

      setShutdownDialogOpen(false)
      fetchAgents(page)
    } catch (error: any) {
      console.error("[v0] Error executing shutdown:", error)
      toast.error(error.response?.data?.message || "Não foi possível executar o comando.")
    } finally {
      setExecuting(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Header com botões */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Gerenciamento de Máquinas</h1>
          <p className="text-muted-foreground mt-2">Visualize e controle todas as máquinas conectadas</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => fetchAgents(page)} disabled={loading}>
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Atualizar
          </Button>
          <Button variant="outline" onClick={() => openShutdownDialog("group")}>
            <Power className="mr-2 h-4 w-4" />
            Desligar Grupo
          </Button>
          <Button variant="destructive" onClick={() => openShutdownDialog("all")}>
            <PowerOff className="mr-2 h-4 w-4" />
            Desligar Todas
          </Button>
        </div>
      </div>

      <AgentStats total={stats.total} online={stats.online} offline={stats.offline} new={stats.new} />

      <Tabs defaultValue="all" className="space-y-4">
        <TabsList>
          <TabsTrigger value="all" className="gap-2">
            <Server className="h-4 w-4" />
            Todas as Máquinas
          </TabsTrigger>
        </TabsList>

        <TabsContent value="all" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Máquinas Conectadas</CardTitle>
              <CardDescription>Lista de todas as máquinas registradas no sistema</CardDescription>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                </div>
              ) : agents.length === 0 ? (
                <div className="text-center py-8">
                  <Server className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                  <p className="text-muted-foreground">Nenhuma máquina registrada no momento</p>
                </div>
              ) : (
                <>
                  <div className="rounded-md border">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Hostname</TableHead>
                          <TableHead>Grupo</TableHead>
                          <TableHead>Status</TableHead>
                          <TableHead>Último Heartbeat</TableHead>
                          <TableHead className="text-right">Ações</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {agents.map((agent) => (
                          <TableRow key={agent.id}>
                            <TableCell className="font-medium font-mono text-sm">{agent.hostname}</TableCell>
                            <TableCell>
                              <Badge variant="outline">{agent.group}</Badge>
                            </TableCell>
                            <TableCell>{getStatusBadge(agent.status)}</TableCell>
                            <TableCell className="text-sm text-muted-foreground">
                              {formatDate(agent.lastHeartbeat)}
                            </TableCell>
                            <TableCell className="text-right">
                              <Button
                                size="sm"
                                variant="destructive"
                                onClick={() => openShutdownDialog("single", agent)}
                              >
                                <PowerOff className="mr-2 h-4 w-4" />
                                Desligar
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>

                  {/* Paginação */}
                  {totalPages > 1 && (
                    <div className="flex items-center justify-between mt-4">
                      <p className="text-sm text-muted-foreground">
                        Página {page + 1} de {totalPages}
                      </p>
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setPage((p) => Math.max(0, p - 1))}
                          disabled={page === 0}
                        >
                          <ChevronLeft className="h-4 w-4 mr-1" />
                          Anterior
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                          disabled={page >= totalPages - 1}
                        >
                          Próxima
                          <ChevronRight className="h-4 w-4 ml-1" />
                        </Button>
                      </div>
                    </div>
                  )}
                </>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Dialog de Desligamento */}
      <Dialog open={shutdownDialogOpen} onOpenChange={setShutdownDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {shutdownType === "single" && "Desligar Máquina"}
              {shutdownType === "group" && "Desligar Grupo"}
              {shutdownType === "all" && "Desligar Todas as Máquinas"}
            </DialogTitle>
            <DialogDescription>
              {shutdownType === "single" && `Você está prestes a desligar a máquina ${selectedAgent?.hostname}`}
              {shutdownType === "group" && "Selecione o grupo de máquinas que deseja desligar"}
              {shutdownType === "all" && "ATENÇÃO: Você está prestes a desligar TODAS as máquinas do sistema"}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            {shutdownType === "group" && (
              <div className="space-y-2">
                <Label htmlFor="group">Grupo</Label>
                <Select value={selectedGroup} onValueChange={setSelectedGroup}>
                  <SelectTrigger id="group">
                    <SelectValue placeholder="Selecione um grupo" />
                  </SelectTrigger>
                  <SelectContent>
                    {uniqueGroups.map((group) => (
                      <SelectItem key={group} value={group}>
                        {group}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="command">Tipo de Comando</Label>
              <Select value={shutdownCommand} onValueChange={setShutdownCommand}>
                <SelectTrigger id="command">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="shutdown">Shutdown</SelectItem>
                  <SelectItem value="restart">Restart</SelectItem>
                  <SelectItem value="hibernate">Hibernate</SelectItem>
                  <SelectItem value="sleep">Sleep</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Checkbox Agendar */}
            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="scheduleCheck"
                checked={scheduleChecked}
                onChange={(e) => setScheduleChecked(e.target.checked)}
              />
              <Label htmlFor="scheduleCheck">Agendar comando</Label>
            </div>

            {/* Campo de data/hora, visível apenas se checkbox marcada */}
            {scheduleChecked && (
              <div className="space-y-2">
                <Label htmlFor="scheduledTime">Agendar Para</Label>
                <input
                  type="datetime-local"
                  id="scheduledTime"
                  className="w-full border rounded-md p-2"
                  value={scheduledTime}
                  onChange={(e) => setScheduledTime(e.target.value)}
                />
              </div>
            )}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setShutdownDialogOpen(false)} disabled={executing}>
              Cancelar
            </Button>
            <Button
              variant="destructive"
              onClick={executeShutdown}
              disabled={executing || (shutdownType === "group" && !selectedGroup)}
            >
              {executing ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Executando...
                </>
              ) : (
                <>
                  <PowerOff className="mr-2 h-4 w-4" />
                  Confirmar
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
