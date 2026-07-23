export default function ErrorState({ message }: { message: string }) {
  return (
    <div className="error-box" role="alert">
      Não foi possível carregar os dados: {message}
    </div>
  );
}
