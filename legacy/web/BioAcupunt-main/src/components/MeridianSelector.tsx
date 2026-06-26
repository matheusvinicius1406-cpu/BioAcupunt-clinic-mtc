import { meridianos } from "../utils/meridianos";

export function MeridianSelector({ values, onChange }: any) {
  return (
    <div className="p-2 border rounded">
      {meridianos.map(m => (
        <div key={m.id} className="mb-2 border-b pb-2">
          <p className="font-bold">{m.nome} ({m.id})</p>
          <select onChange={(e) => onChange(m.id, { ...values[m.id], status: e.target.value })}>
            <option value="normal">Normal</option>
            <option value="excesso">Excesso</option>
            <option value="deficiencia">Deficiência</option>
          </select>
        </div>
      ))}
    </div>
  );
}
