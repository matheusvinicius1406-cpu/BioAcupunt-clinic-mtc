import { useRef } from "react";
import SignatureCanvas from "react-signature-canvas";

export function SignaturePad({ onSave }: { onSave: (data: string) => void }) {
  const sigCanvas = useRef<SignatureCanvas>(null);
  
  const clear = () => {
    sigCanvas.current?.clear();
  };

  const save = () => {
    if (sigCanvas.current) {
      onSave(sigCanvas.current.getTrimmedCanvas().toDataURL('image/png'));
    }
  };

  return (
    <div className="border p-4 bg-gray-50 rounded-2xl">
      <div className="bg-white border-2 border-dashed border-gray-200 rounded-xl overflow-hidden">
        <SignatureCanvas 
          ref={sigCanvas}
          penColor="black"
          canvasProps={{
            className: "w-full min-h-[150px] cursor-crosshair"
          }} 
        />
      </div>
      <div className="mt-4 flex gap-3">
        <button 
          onClick={clear} 
          type="button"
          className="flex-1 py-2 text-sm font-bold text-gray-500 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
        >
          Limpar
        </button>
        <button 
          onClick={save} 
          type="button"
          className="flex-1 py-2 text-sm font-bold text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors shadow-sm"
        >
          Confirmar Assinatura
        </button>
      </div>
    </div>
  );
}
