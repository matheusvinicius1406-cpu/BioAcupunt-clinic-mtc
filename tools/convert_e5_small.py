#!/usr/bin/env python3
"""
CONVERSÃO DO multilingual-e5-small PARA TFLite (INT8)

Uso:
    python tools/convert_e5_small.py [--output-dir app/src/main/assets/models/]

Gera dois arquivos:
    - e5-small-int8.tflite   (INT8 quantizado, ~45 MB, recomendado)
    - e5-small-fp16.tflite   (FP16 quantizado, ~90 MB, fallback)

Requisitos:
    pip install sentence-transformers tensorflow onnx onnx2tf

Referência:
    https://huggingface.co/intfloat/multilingual-e5-small
"""

import argparse
import hashlib
import json
import os
import sys
from pathlib import Path

# ==============================================================================
# Configuração
# ==============================================================================

MODEL_NAME = "intfloat/multilingual-e5-small"
EXPECTED_DIMENSIONS = 384
DEFAULT_OUTPUT_DIR = Path("app/src/main/assets/models")


def parse_args():
    parser = argparse.ArgumentParser(description="Converte e5-small para TFLite")
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=DEFAULT_OUTPUT_DIR,
        help=f"Diretório de saída (default: {DEFAULT_OUTPUT_DIR})",
    )
    parser.add_argument(
        "--quantization",
        choices=["int8", "fp16", "both"],
        default="both",
        help="Tipo de quantização (default: both)",
    )
    parser.add_argument(
        "--verify",
        action="store_true",
        default=True,
        help="Verifica dimensões e similaridade após conversão",
    )
    return parser.parse_args()


# ==============================================================================
# Passo 1: Exportar para ONNX via sentence-transformers
# ==============================================================================

def export_to_onnx(output_dir: Path) -> Path:
    """Exporta o modelo para o formato ONNX intermediário."""
    print(f"[1/4] Carregando modelo {MODEL_NAME}...")
    from sentence_transformers import SentenceTransformer

    model = SentenceTransformer(MODEL_NAME)
    print(f"  Modelo carregado: {MODEL_NAME}")
    print(f"  Dimensões: {model.get_sentence_embedding_dimension()}")

    assert model.get_sentence_embedding_dimension() == EXPECTED_DIMENSIONS, (
        f"Dimensão esperada {EXPECTED_DIMENSIONS}, mas modelo retorna "
        f"{model.get_sentence_embedding_dimension()}"
    )

    onnx_path = output_dir / "e5-small.onnx"
    print(f"  Exportando para ONNX: {onnx_path}")

    # Export via sentence-transformers
    model.export(filepath=str(onnx_path), format="onnx")

    print(f"  ONNX exportado com sucesso: {onnx_path}")
    return onnx_path


# ==============================================================================
# Passo 2: Converter ONNX → TensorFlow SavedModel
# ==============================================================================

def convert_onnx_to_tf(onnx_path: Path, output_dir: Path) -> Path:
    """Converte ONNX para TensorFlow SavedModel intermediário."""
    import tensorflow as tf

    print(f"[2/4] Convertendo ONNX → TensorFlow SavedModel...")

    # Usar onnx2tf para conversão
    try:
        import onnx2tf
    except ImportError:
        print("  Instalando onnx2tf...")
        os.system("pip install onnx2tf onnx onnxruntime")
        import onnx2tf

    saved_model_dir = output_dir / "e5-small_savedmodel"
    if saved_model_dir.exists():
        import shutil
        shutil.rmtree(saved_model_dir)

    onnx2tf.convert(
        input_onnx_file_path=str(onnx_path),
        output_folder_path=str(saved_model_dir),
        # Remover operações não suportadas pelo TFLite
        replacement_parameters=[
            ("tf.Split", "tf.Slice"),
        ],
    )

    print(f"  SavedModel criado: {saved_model_dir}")
    return saved_model_dir


# ==============================================================================
# Passo 3: Converter SavedModel → TFLite (INT8 + FP16)
# ==============================================================================

def convert_to_tflite(
    saved_model_dir: Path,
    output_dir: Path,
    quantizations: list[str],
) -> dict[str, Path]:
    """Converte SavedModel para TFLite nos formatos solicitados."""
    import tensorflow as tf

    print(f"[3/4] Convertendo para TFLite...")

    results = {}

    # Dataset de calibração para INT8 (amostras representativas)
    calibration_texts = [
        "query: acupuntura para lombalgia crônica",
        "query: pontos de meridiano do pulmão",
        "query: contraindicações da gestação na acupuntura",
        "query: tratamento de ansiedade com fitoterapia chinesa",
        "passage: A acupuntura é uma técnica da Medicina Tradicional Chinesa",
        "passage: O ponto LI4 (Hegu) é contraindicado em gestantes",
        "passage: A fitoterapia chinesa utiliza fórmulas combinadas",
        "passage: O meridiano do Pulmão (Taiyin) inicia-se no tórax",
    ]

    def representative_dataset():
        import numpy as np
        for text in calibration_texts:
            # Input shape: [1, 512] — batch=1, tokens=512
            input_data = np.zeros((1, 512), dtype=np.float32)
            for i, b in enumerate(text.encode("utf-8")[:511]):
                input_data[0, i + 1] = b / 255.0
            input_data[0, 0] = 101.0  # CLS token
            yield [input_data]

    if "int8" in quantizations or "both" in quantizations:
        print("  Convertendo INT8...")
        converter = tf.lite.TFLiteConverter.from_saved_model(str(saved_model_dir))
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.representative_dataset = representative_dataset
        converter.target_spec.supported_types = [tf.float32]
        converter.inference_input_type = tf.float32
        converter.inference_output_type = tf.float32

        tflite_int8 = converter.convert()
        int8_path = output_dir / "e5-small-int8.tflite"
        int8_path.write_bytes(tflite_int8)
        results["int8"] = int8_path
        print(f"  INT8 criado: {int8_path} ({int8_path.stat().st_size / 1024 / 1024:.1f} MB)")

    if "fp16" in quantizations or "both" in quantizations:
        print("  Convertendo FP16...")
        converter = tf.lite.TFLiteConverter.from_saved_model(str(saved_model_dir))
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float16]

        tflite_fp16 = converter.convert()
        fp16_path = output_dir / "e5-small-fp16.tflite"
        fp16_path.write_bytes(tflite_fp16)
        results["fp16"] = fp16_path
        print(f"  FP16 criado: {fp16_path} ({fp16_path.stat().st_size / 1024 / 1024:.1f} MB)")

    return results


# ==============================================================================
# Passo 4: Verificar integridade e gerar hashes
# ==============================================================================

def verify_and_hash(models: dict[str, Path], output_dir: Path):
    """Verifica os modelos e gera hashes SHA-256 para fixar no código."""
    print(f"[4/4] Verificando modelos e gerando hashes...")

    hashes = {}

    for name, path in models.items():
        # SHA-256
        sha256 = hashlib.sha256(path.read_bytes()).hexdigest()

        # Tamanho
        size = path.stat().st_size

        hashes[name] = {
            "path": str(path),
            "size_bytes": size,
            "size_mb": round(size / 1024 / 1024, 1),
            "sha256": sha256,
        }

        print(f"\n  [{name.upper()}]")
        print(f"    Path: {path}")
        print(f"    Size: {hashes[name]['size_mb']} MB")
        print(f"    SHA-256: {sha256}")

        # Verificar dimensões com onnxruntime
        if args.verify:
            try:
                import onnxruntime as ort
                import numpy as np

                session = ort.InferenceSession(
                    str(output_dir / "e5-small.onnx"),
                    providers=["CPUExecutionProvider"],
                )
                input_name = session.get_inputs()[0].name
                output_name = session.get_outputs()[0].name

                # Test input
                test_input = np.zeros((1, 512), dtype=np.float32)
                test_input[0, 0] = 101.0  # CLS
                for i, b in enumerate(b"query: teste de acupuntura"):
                    test_input[0, i + 1] = b / 255.0
                test_input[0, 13] = 102.0  # SEP

                output = session.run([output_name], {input_name: test_input})
                dims = output[0].shape[-1]
                print(f"    Dimensões verificadas: {dims} (esperado: {EXPECTED_DIMENSIONS})")
                assert dims == EXPECTED_DIMENSIONS, f"Dimensão {dims} != {EXPECTED_DIMENSIONS}"

            except ImportError:
                print("    Pulando verificação de dimensões (onnxruntime não disponível)")

    # Gerar arquivo de hashes para copiar para o código
    hashes_file = output_dir / "model_hashes.json"
    hashes_file.write_text(json.dumps(hashes, indent=2))
    print(f"\n  Hashes salvos em: {hashes_file}")

    # Imprimir constantes Kotlin para copiar para EmbeddingModelCatalog
    print("\n\n  === Constantes Kotlin para EmbeddingModelCatalog ===\n")
    for name, info in hashes.items():
        model_id = f"e5-small-{name}"
        print(f'    EmbeddingModel(')
        print(f'        id = "{model_id}",')
        print(f'        fileName = "{Path(info[\"path\"]).name}",')
        print(f'        sizeBytes = {info["size_bytes"]}L,')
        print(f'        sha256 = "{info["sha256"]}",')
        print(f'    ),')
    print()


# ==============================================================================
# Main
# ==============================================================================

if __name__ == "__main__":
    args = parse_args()

    output_dir = args.output_dir
    output_dir.mkdir(parents=True, exist_ok=True)

    quantizations = ["int8", "fp16"] if args.quantization == "both" else [args.quantization]

    try:
        # Passo 1: Exportar para ONNX
        onnx_path = export_to_onnx(output_dir)

        # Passo 2: Converter para TF SavedModel
        saved_model_dir = convert_onnx_to_tf(onnx_path, output_dir)

        # Passo 3: Converter para TFLite
        models = convert_to_tflite(saved_model_dir, output_dir, quantizations)

        # Passo 4: Verificar e hashear
        verify_and_hash(models, output_dir)

        print("\n✅ Conversão concluída com sucesso!")
        print(f"   Modelos em: {output_dir.resolve()}")
        print(f"\n   Próximo passo: copiar os arquivos .tflite para o dispositivo")
        print(f"   e executar ./scripts/pin_models.sh para fixar os hashes no código.")

    except Exception as e:
        print(f"\n❌ Erro durante a conversão: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)
