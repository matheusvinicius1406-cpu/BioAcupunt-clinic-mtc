import ErrorState from "@/components/ErrorState";
import { apiGet, BackendError } from "@/lib/backend";
import type { LibraryNode } from "@/lib/types";

export const dynamic = "force-dynamic";

function tagList(tags: string): string[] {
  return tags
    .split(/[,;]/)
    .map((t) => t.trim())
    .filter(Boolean);
}

export default async function BibliotecaPage() {
  let nodes: LibraryNode[];
  try {
    nodes = await apiGet<LibraryNode[]>("/api/v1/library");
  } catch (e) {
    const msg = e instanceof BackendError ? e.message : "erro inesperado";
    return (
      <>
        <div className="page-head">
          <h1>Biblioteca</h1>
        </div>
        <ErrorState message={msg} />
      </>
    );
  }

  return (
    <>
      <div className="page-head">
        <h1>Biblioteca</h1>
        <p>Acervo de referência clínica, somente leitura.</p>
      </div>

      {nodes.length === 0 ? (
        <div className="card card-pad">
          <div className="empty">
            <span className="emoji">▧</span>
            O acervo ainda está vazio no backend.
          </div>
          <p
            style={{
              color: "var(--text-faint)",
              fontSize: 13,
              marginTop: 12,
              textAlign: "center",
            }}
          >
            Conteúdo entra por curadoria humana via pipeline de ingestão — nunca é
            gerado nem servido automaticamente. Enquanto a ingestão não popular a
            biblioteca do servidor, o acervo curado vive no aplicativo Android.
          </p>
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          {nodes.map((n) => (
            <div key={n.id} className="card card-pad">
              <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                <h2 style={{ fontSize: 16 }}>{n.title}</h2>
                {n.type ? (
                  <span className="badge badge-neutral" style={{ flexShrink: 0 }}>
                    {n.type}
                  </span>
                ) : null}
              </div>
              {n.summary ? (
                <p style={{ color: "var(--text-muted)", fontSize: 14, marginTop: 6 }}>
                  {n.summary}
                </p>
              ) : null}
              {tagList(n.tags).length > 0 ? (
                <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginTop: 10 }}>
                  {tagList(n.tags).map((t) => (
                    <span key={t} className="badge badge-info">
                      {t}
                    </span>
                  ))}
                </div>
              ) : null}
            </div>
          ))}
        </div>
      )}
    </>
  );
}
