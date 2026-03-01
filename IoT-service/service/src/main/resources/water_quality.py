import numpy as np
import sys

def calculate_water_quality(turbidity, ph, chlorine):
    turbidity = float(turbidity)
    ph = float(ph)
    chlorine = float(chlorine)

    # 定义理想值和最大偏差
    ph_ideal = 7.0
    turbidity_max = 2.0   # 最大可接受浊度 (NTU)
    ph_deviation = 1.5
    chlorine_deviation = 0.7
    chlorine_ideal = 0.3

    # 标准化各个指标（统一量纲）
    T = 1 - turbidity / turbidity_max
    P = 1 - abs(ph - ph_ideal) / ph_deviation
    C = 1 - abs(chlorine - chlorine_ideal) / chlorine_deviation

    # 确保值在 [0, 1] 范围内
    T = max(0.0, min(1.0, T))
    P = max(0.0, min(1.0, P))
    C = max(0.0, min(1.0, C))

    scores = np.array([T, P, C])

    # 若所有指标均为0，水质极差
    if scores.sum() == 0:
        return 0.0

    # 归一化得到权重分布 p
    p = scores / scores.sum()

    # 计算信息熵（以指标数量3为底）
    p_safe = np.where(p > 0, p, 1e-10)
    entropy = -np.sum(p_safe * np.log(p_safe)) / np.log(3)

    # 计算差异系数与熵权
    diff = 1 - entropy
    if diff == 0:
        # 熵最大时各指标同等重要，使用均等权重
        weights = np.array([1/3, 1/3, 1/3])
    else:
        # 熵越小，说明该分布差异越大，权重越高
        # 此处为单样本简化版，对多指标分配权重
        raw_weights = 1 - p_safe * np.log(p_safe) / np.log(3) / (entropy + 1e-10)
        weights = raw_weights / raw_weights.sum()

    # 计算加权 WQI
    WQI = float(np.dot(weights, scores))
    WQI = max(0.0, min(1.0, WQI))

    return WQI

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python water_quality.py <turbidity> <ph> <chlorine>")
        sys.exit(1)

    result = calculate_water_quality(sys.argv[1], sys.argv[2], sys.argv[3])
    print(f"{result:.4f}")