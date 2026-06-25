export function PaymentMethodSelector({ value, onChange }: { value: string, onChange: (v: string) => void }) {
  return (
    <select value={value} onChange={(e) => onChange(e.target.value)} className="w-full border p-2">
      <option value="pix">PIX</option>
      <option value="cartao">Cartão</option>
      <option value="dinheiro">Dinheiro</option>
    </select>
  );
}
