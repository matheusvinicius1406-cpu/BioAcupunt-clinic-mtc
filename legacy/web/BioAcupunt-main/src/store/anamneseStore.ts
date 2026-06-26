import { create } from 'zustand';

interface AnamneseState {
  currentAnamnesisId: string | null;
  anamneseData: {
    id: string;
    patientId: string;
    date: string;
    queixa: { 
      principal: string; 
      inicio: string; 
      cronologia: string; 
      gatilho: string; 
      evolucao: string 
    };
    mapaDor: any[];
    moduladores: { melhora: string[]; piora: string[] };
    baGang: {
      termorregulacao: string[];
      transpiracao: string[];
      sono: string[];
      digestao: string[];
      gosto: string[];
      fezes: string[];
      urina: string[];
      ciclo: string[];
      fluxo: string;
      emocional: string[];
      energia: string[];
      notas: string;
    };
    lingua: { 
      cor: string; 
      forma: string; 
      marcas: string; 
      saburraCor: string; 
      saburraEspessura: string; 
      saburraDistribuicao: string; 
      umidade: string; 
      obs: string 
    };
    pulso: {
      esquerdocun: string[];
      esquerdoguan: string[];
      esquerdochi: string[];
      direitocun: string[];
      direitoguan: string[];
      direitochi: string[];
      frequencia: string;
      forca: string;
      impressao: string;
    };
    plano: { 
      meridianos: string[];
      pontos: string[]; 
      tecnicas: string[]; 
      orientacoes: string;
      evolucao: string;
      evaSaida: number;
    };
    currentDiagnosis?: any;
  };
  clinicalHistory: any[];
  updateField: (module: string, field: string, value: any) => void;
  setCurrentDiagnosis: (diag: any) => void;
  setAnamnesis: (data: any) => void;
  setHistory: (history: any[]) => void;
}

export const useAnamneseStore = create<AnamneseState>((set) => ({
  currentAnamnesisId: null,
  anamneseData: {
    id: '',
    patientId: '',
    date: new Date().toISOString(),
    queixa: {
      principal: '',
      inicio: '',
      cronologia: '',
      gatilho: '',
      evolucao: '',
    },
    mapaDor: [],
    moduladores: {
      melhora: [],
      piora: []
    },
    baGang: {
      termorregulacao: [],
      transpiracao: [],
      sono: [],
      digestao: [],
      gosto: [],
      fezes: [],
      urina: [],
      ciclo: [],
      fluxo: '',
      emocional: [],
      energia: [],
      notas: ''
    },
    lingua: {
      cor: '',
      forma: '',
      marcas: '',
      saburraCor: '',
      saburraEspessura: '',
      saburraDistribuicao: '',
      umidade: '',
      obs: ''
    },
    pulso: {
      esquerdocun: [],
      esquerdoguan: [],
      esquerdochi: [],
      direitocun: [],
      direitoguan: [],
      direitochi: [],
      frequencia: '',
      forca: '',
      impressao: ''
    },
    plano: {
      meridianos: [],
      pontos: [],
      tecnicas: [],
      orientacoes: '',
      evolucao: '',
      evaSaida: 0
    },
    currentDiagnosis: null
  },
  clinicalHistory: [],
  updateField: (module, field, value) => set((state) => {
    const key = module as keyof typeof state.anamneseData;
    const currentVal = state.anamneseData[key];

    // If field is not provided, or current value is not an object, or it's an array,
    // we replace the entire value for that module/field.
    if (!field || typeof currentVal !== 'object' || currentVal === null || Array.isArray(currentVal)) {
      return {
        anamneseData: {
          ...state.anamneseData,
          [module]: value
        }
      };
    }

    return {
      anamneseData: {
        ...state.anamneseData,
        [module]: { ...currentVal, [field]: value }
      }
    };
  }),
  setCurrentDiagnosis: (diag) => set((state) => ({ anamneseData: { ...state.anamneseData, currentDiagnosis: diag } })),
  setAnamnesis: (data) => set({ anamneseData: data, currentAnamnesisId: data.id }),
  setHistory: (history) => set({ clinicalHistory: history }),
}));
