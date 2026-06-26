export function PackageCard({ pkg }: { pkg: any }) {
  return (
    <div className="border p-4 rounded shadow-sm bg-white mb-2">
      <h3 className="font-bold">{pkg.name}</h3>
      <p>{pkg.totalSessions} sessões - R$ {pkg.price}</p>
    </div>
  );
}
