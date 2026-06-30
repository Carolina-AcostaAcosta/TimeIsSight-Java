package com.glaucoma.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Paciente {
  private String id;       // Identificador del paciente i
  private int ti;          // Tiempo de llegada al sistema (en minutos de la simulación)
  private int gi;          // Nivel de gravedad (0: no glaucoma, 1: sospecha, 2: moderado, 3: avanzado, 4: agudo)
  private int di;          // Plazo crítico máximo permitido en minutos (convertido de días)

  private boolean entraPorUrgencias;
  private boolean esGlaucoma;
  private boolean seDerivaCHUC;
  private boolean necesitaSeguimiento;
  private int caeAsignado;
  private int totalDias;
  
  @JsonIgnore
  private List<Cita> circuito;
  
  public Paciente() {
    this.circuito = new ArrayList<>();
  }
  
  public Paciente(String id, int ti, int gi, int di, boolean entraPorUrgencias, boolean esGlaucoma, boolean seDerivaCHUC, boolean necesitaSeguimiento, int caeAsignado, int totalDias) {
    this.id = id;
    this.ti = ti;
    this.gi = gi;
    this.di = di;
    this.entraPorUrgencias = entraPorUrgencias;
    this.esGlaucoma = esGlaucoma;
    this.seDerivaCHUC = seDerivaCHUC;
    this.necesitaSeguimiento = necesitaSeguimiento;
    this.caeAsignado = caeAsignado;
    this.totalDias = totalDias;
    this.circuito = new ArrayList<>();
  }
  
  // Getters
  public String getId() { return id; }
  public int getTi() { return ti; }
  public int getGi() { return gi; }
  public int getDi() { return di; }
  public boolean isEntraPorUrgencias() { return entraPorUrgencias; }
  public boolean isEsGlaucoma() { return esGlaucoma; }
  public boolean isNecesitaSeguimiento() { return necesitaSeguimiento; }
  public boolean isSeDerivaCHUC() { return seDerivaCHUC; }
  public int getCaeAsignado() { return caeAsignado; }
  public int getTotalDias() { return totalDias; }
  
  @JsonIgnore
  public List<Cita> getCircuito() { return circuito; }
  
  public void generarCircuito(List<Estacion> estaciones, List<Recurso> recursos, boolean paralela, int diasParalelaMes, int finSimulacion) {
    this.circuito = new ArrayList<>();
    int idIncremental = 1;
    
    Random rand = new Random(this.id.hashCode());
    
    Estacion eUrgencias = estaciones.get(0);
    Estacion ePruebas = estaciones.get(1);
    Estacion eCae = estaciones.get(2);
    Estacion eChuc = estaciones.get(3);
    Estacion eGlaucoma = paralela ? estaciones.get(4) : null;
    
    // Filtramos los doctores correspondientes a su CAE asignado
    List<Recurso> doctoresCAE = recursos.stream().filter(r -> r.id().startsWith("R_CAE_")).toList();
    List<Recurso> misDoctoresCAE = doctoresCAE.subList((this.caeAsignado - 1) * 2, caeAsignado * 2);
    
    List<Recurso> doctoresCHUC = recursos.stream().filter(r -> r.id().startsWith("R_CHUC_")).toList();
    
    // Máquinas específicas del CAE asignado
    Recurso rOctCAE = recursos.stream().filter(r -> r.id().equals("R_OCT_CAE_" + this.caeAsignado)).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_OCT_CAE_" + this.caeAsignado + " en la configuración"));
    Recurso rCampCAE = recursos.stream().filter(r -> r.id().equals("R_Camp_CAE_" + this.caeAsignado)).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Camp_CAE_" + this.caeAsignado + " en la configuración"));
    Recurso rTonoCAE = recursos.stream().filter(r -> r.id().equals("R_Tono_CAE_" + this.caeAsignado)).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Tono_CAE_" + this.caeAsignado + " en la configuración"));
    Recurso rRetinoCAE = recursos.stream().filter(r -> r.id().equals("R_Retino_CAE_" + this.caeAsignado)).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Retino_CAE_" + this.caeAsignado + " en la configuración"));
    
    if (this.entraPorUrgencias) {
      Recurso rTriaje = recursos.stream().filter(r -> r.id().equals("R_Triaje")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Triaje en la configuración"));
      this.circuito.add(nuevaCita(idIncremental++, eUrgencias, rTriaje, 20, false, finSimulacion, diasParalelaMes));
    } else if (this.gi > 2) {
      // Validación en el CAE por un médico, sin asistencia del paciente
      Recurso docCAE = misDoctoresCAE.get(rand.nextInt(misDoctoresCAE.size()));
      this.circuito.add(nuevaCita(idIncremental++, eCae, docCAE, 5, false, finSimulacion, diasParalelaMes));
    }
    
    // 3. PRUEBAS DIAGNÓSTICAS
    this.circuito.add(nuevaCita(idIncremental++, ePruebas, rTonoCAE, 5, false, finSimulacion, diasParalelaMes));
    this.circuito.add(nuevaCita(idIncremental++, ePruebas, rRetinoCAE, 15, false, finSimulacion, diasParalelaMes));
    
    if (this.isEsGlaucoma()) { // Sospecha o diagnóstico positivo
      this.circuito.add(nuevaCita(idIncremental++, ePruebas, rOctCAE, 15, false, finSimulacion, diasParalelaMes));
      this.circuito.add(nuevaCita(idIncremental++, ePruebas, rCampCAE, 15, false, finSimulacion, diasParalelaMes));
    }
    
    // 4. ENRUTAMIENTO MÉDICO (Árbol de decisión)
    
    // CASO A: Agenda Paralela activa y paciente con sospecha/glaucoma
    if (paralela && this.gi > 0) {
      Recurso docMonografico = recursos.stream().filter(r -> r.id().equals("R_GlaucoCAE_" + this.caeAsignado)).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_GlaucoCAE_" + this.caeAsignado + " en la configuración"));
      this.circuito.add(nuevaCita(idIncremental++, eGlaucoma, docMonografico, 15, true, finSimulacion, diasParalelaMes));
    }
    // CASO B: Agenda Normal (o paciente sano en agenda paralela)
    else {
      Recurso docCAE = misDoctoresCAE.get(rand.nextInt(misDoctoresCAE.size()));
      
      if (this.seDerivaCHUC) {
        // Sub-caso B.1: Derivado al hospital central
        this.circuito.add(nuevaCita(idIncremental++, eCae, docCAE, 30, false, finSimulacion, diasParalelaMes));
        
        Recurso rTonoHUC = recursos.stream().filter(r -> r.id().equals("R_Tono_CHUC")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Tono_HUC en la configuración"));
        Recurso rRetinoHUC = recursos.stream().filter(r -> r.id().equals("R_Retino_CHUC")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Retino_HUC en la configuración"));
        Recurso rOctHUC = recursos.stream().filter(r -> r.id().equals("R_OCT_CHUC")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_OCT_HUC en la configuración"));
        Recurso rCampHUC = recursos.stream().filter(r -> r.id().equals("R_Camp_CHUC")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Camp_HUC en la configuración"));
        
        this.circuito.add(nuevaCita(idIncremental++, ePruebas, rTonoHUC, 5, false, finSimulacion, diasParalelaMes));
        this.circuito.add(nuevaCita(idIncremental++, ePruebas, rRetinoHUC, 15, false, finSimulacion, diasParalelaMes));
        if (this.esGlaucoma) {
          this.circuito.add(nuevaCita(idIncremental++, ePruebas, rOctHUC, 15, false, finSimulacion, diasParalelaMes));
          this.circuito.add(nuevaCita(idIncremental++, ePruebas, rCampHUC, 15, false, finSimulacion, diasParalelaMes));
        }
        
        Recurso docCHUC = doctoresCHUC.get(rand.nextInt(doctoresCHUC.size()));
        this.circuito.add(nuevaCita(idIncremental++, eChuc, docCHUC, 30, true, finSimulacion, diasParalelaMes));
        
        if (this.necesitaSeguimiento) {
          this.circuito.add(nuevaCita(idIncremental++, eChuc, docCHUC, 15, false, finSimulacion, diasParalelaMes));
        }
      } else {
        // Subcaso B.2: Resuelto en el ambulatorio periférico
        this.circuito.add(nuevaCita(idIncremental++, eCae, docCAE, 30, true, finSimulacion, diasParalelaMes));
        
        if (this.necesitaSeguimiento) {
          this.circuito.add(nuevaCita(idIncremental++, eCae, docCAE, 15, false, finSimulacion, diasParalelaMes));
        }
      }
    }
  }
  
  private Cita nuevaCita(int indice, Estacion estacion, Recurso recurso, int duracion, boolean diagnosticoFinal, int finSimulacion, int diasParalelaMes) {
    String idCita = this.id + "-C" + indice;
    return new Cita(idCita, this, estacion, recurso, duracion, diagnosticoFinal, finSimulacion, diasParalelaMes);
  }
}