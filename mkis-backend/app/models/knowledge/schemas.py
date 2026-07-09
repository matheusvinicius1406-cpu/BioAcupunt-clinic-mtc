[itats]
id = Column(UUID(as_uuid=True), primary_key=True, default=uuid4)
schema_name = Column(String(255), nullable=False)
table_name = Column(String(255), nullable=False)
start_value = Column(BigInteger, nullable=False)
increment_by = Column(Integer, nullable=False, default=1)
max_value = Column(BigInteger, nullable=True)
cycle = Column(Boolean, nullable=False, default=False)
owner = Column(String(255), nullable=True)


@as_declarative()
class Base:
    metadata = MetaData(schema="mkis")
    registry = registry()
