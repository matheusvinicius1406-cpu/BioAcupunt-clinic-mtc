export function DiagnosisCard({ diagnosis }: { diagnosis: any }) {
  return (
    <div className="border p-4 rounded shadow bg-white">
      <h3 className="font-bold text-lg">Diagnóstico MTC</h3>
      <p>{diagnosis.treatment}</p>
      <p className="text-sm text-gray-600">{diagnosis.rationale}</p>
    </div>
  );
}
