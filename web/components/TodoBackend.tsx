// Aviso honesto: a tela existe no painel, mas o endpoint correspondente ainda
// nao existe no backend FastAPI. Nada e inventado ou simulado aqui.
export default function TodoBackend({
  what,
  endpoint,
}: {
  what: string;
  endpoint: string;
}) {
  return (
    <div className="notice">
      <span className="tag">TODO backend</span>
      <p style={{ margin: 0 }}>
        <strong>{what}</strong> ainda não tem endpoint no backend. O painel web só
        exibe dados que a API realmente expõe — nada é simulado.
      </p>
      <p style={{ margin: "10px 0 0", fontSize: 13 }}>
        Falta implementar em <code>{endpoint}</code>. Assim que existir, esta tela
        passa a consumi-lo.
      </p>
    </div>
  );
}
