import tkinter as tk
from tkinter import messagebox
import serial
import time
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
from collections import deque

class ConsolaESP32:
    def __init__(self):
        self.ser = None
        self.conectado = False
        self.datos_rpm = deque(maxlen=200)
        self.datos_tiempo = deque(maxlen=200)
        self.tiempo_inicio = time.time()

        self.ultimo_rpm = 0.0
        self.datos_reales_disponibles = False

        # Variables para reducir saturación
        self.line_skip = 3  # Mostrar 1 de cada 3 líneas
        self.contador_lineas = 0

        self.setup_serial()
        self.setup_gui()
        self.iniciar_monitoreo()

    def setup_serial(self):
        try:
            self.ser = serial.Serial('COM5', 115200, timeout=0.1)
            time.sleep(2)
            self.conectado = True
            print("✅ Conexión serial establecida en COM5")
        except Exception as e:
            messagebox.showerror("Error", f"No se pudo conectar al puerto COM5:\n{e}")
            self.conectado = False

    def setup_gui(self):
        self.root = tk.Tk()
        self.root.title("Pintaditos S.A. - Consola ESP32")
        self.root.geometry("1200x700")  # Más ancho para la gráfica

        main_frame = tk.Frame(self.root)
        main_frame.pack(expand=True, fill='both', padx=10, pady=10)

        # ====== Consola ======
        consola_frame = tk.LabelFrame(main_frame, text="Consola ESP32", font=("Arial", 12, "bold"))
        consola_frame.pack(side=tk.LEFT, fill='both', expand=True, padx=5)

        self.text_area = tk.Text(consola_frame, font=("Consolas", 14))
        self.text_area.pack(side="left", fill="both", expand=True)
        scrollbar = tk.Scrollbar(consola_frame, command=self.text_area.yview)
        scrollbar.pack(side="right", fill="y")
        self.text_area.config(yscrollcommand=scrollbar.set)

        # ====== Gráfica ======
        grafica_frame = tk.LabelFrame(main_frame, text="Gráfica de RPM en Tiempo Real", font=("Arial", 12, "bold"))
        grafica_frame.pack(side=tk.RIGHT, fill='both', expand=True, padx=5)

        # Agrandar figura y ocupar todo el frame
        self.fig, self.ax = plt.subplots(figsize=(8, 6))
        self.ax.set_title("RPM vs Tiempo")
        self.ax.set_xlabel("Tiempo (s)")
        self.ax.set_ylabel("RPM")
        self.ax.grid(True, alpha=0.3)
        self.line, = self.ax.plot([], [], 'b-', linewidth=2)

        self.canvas = FigureCanvasTkAgg(self.fig, grafica_frame)
        self.canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)

    def iniciar_monitoreo(self):
        self.actualizar_monitor()

    def actualizar_monitor(self):
        if self.ser and self.ser.in_waiting > 0:
            try:
                lines = self.ser.read(self.ser.in_waiting).decode(errors="ignore").splitlines()
                for data in lines:
                    if data:
                        self.contador_lineas += 1
                        if self.contador_lineas >= self.line_skip:
                            timestamp = time.strftime("%H:%M:%S")
                            self.text_area.insert(tk.END, f"[{timestamp}] {data}\n")
                            self.text_area.see(tk.END)
                            self.contador_lineas = 0

                        if "RPM:" in data:
                            try:
                                valor = data.split("RPM:")[1].split()[0]
                                self.ultimo_rpm = float(valor)
                                self.datos_reales_disponibles = True
                            except:
                                pass

                # Limitar consola a últimas 200 líneas
                lines_all = self.text_area.get(1.0, tk.END).split("\n")
                if len(lines_all) > 200:
                    self.text_area.delete(1.0, f"{len(lines_all)-200}.0")

            except Exception as e:
                print("Error leyendo serial:", e)

        if self.datos_reales_disponibles:
            self.actualizar_grafica()

        self.root.after(50, self.actualizar_monitor)  # Actualización más lenta para reducir saturación

    def actualizar_grafica(self):
        tiempo_actual = time.time() - self.tiempo_inicio
        self.datos_tiempo.append(tiempo_actual)
        self.datos_rpm.append(self.ultimo_rpm)

        self.line.set_data(list(self.datos_tiempo), list(self.datos_rpm))
        self.ax.set_xlim(max(0, tiempo_actual - 30), tiempo_actual + 2)
        self.ax.set_ylim(0, max(3500, max(self.datos_rpm, default=1000) + 100))

        self.canvas.draw()

    def run(self):
        self.root.mainloop()


if __name__ == "__main__":
    app = ConsolaESP32()
    app.run()
