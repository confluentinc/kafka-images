#!/usr/bin/env python3

"""
Kafka Benchmark Results Analyzer
Analyzes and visualizes benchmark results from JVM vs Native image comparisons
"""

import os
import sys
import json
import glob
import argparse
from pathlib import Path
from datetime import datetime
from statistics import mean, median, stdev
from typing import List, Dict, Tuple

try:
    import matplotlib.pyplot as plt
    import numpy as np
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False
    print("⚠️  matplotlib not available. Install with: pip install matplotlib")

def load_benchmark_data(results_dir: str) -> Dict:
    """Load benchmark data from results directory"""
    data = {
        'jvm': {'startup': [], 'memory': []},
        'native': {'startup': [], 'memory': []}
    }
    
    # Find all result files
    startup_files = glob.glob(f"{results_dir}/*_startup_*.txt")
    memory_files = glob.glob(f"{results_dir}/*_memory_*.txt")
    
    # Load startup times
    for file_path in startup_files:
        file_name = os.path.basename(file_path)
        if 'jvm' in file_name:
            image_type = 'jvm'
        elif 'native' in file_name:
            image_type = 'native'
        else:
            continue
            
        try:
            with open(file_path, 'r') as f:
                values = [float(line.strip()) for line in f if line.strip()]
                data[image_type]['startup'].extend(values)
        except Exception as e:
            print(f"Error reading {file_path}: {e}")
    
    # Load memory usage
    for file_path in memory_files:
        file_name = os.path.basename(file_path)
        if 'jvm' in file_name:
            image_type = 'jvm'
        elif 'native' in file_name:
            image_type = 'native'
        else:
            continue
            
        try:
            with open(file_path, 'r') as f:
                values = [float(line.strip()) for line in f if line.strip()]
                data[image_type]['memory'].extend(values)
        except Exception as e:
            print(f"Error reading {file_path}: {e}")
    
    return data

def calculate_statistics(values: List[float]) -> Dict:
    """Calculate statistics for a list of values"""
    if not values:
        return {'mean': 0, 'median': 0, 'std': 0, 'min': 0, 'max': 0, 'count': 0}
    
    return {
        'mean': mean(values),
        'median': median(values),
        'std': stdev(values) if len(values) > 1 else 0,
        'min': min(values),
        'max': max(values),
        'count': len(values)
    }

def print_statistics(data: Dict):
    """Print statistical analysis of benchmark data"""
    print("🔍 Statistical Analysis")
    print("=" * 50)
    
    for image_type in ['jvm', 'native']:
        print(f"\n📊 {image_type.upper()} Image Results:")
        
        # Startup statistics
        startup_stats = calculate_statistics(data[image_type]['startup'])
        print(f"  🚀 Startup Time (seconds):")
        print(f"     Mean: {startup_stats['mean']:.3f}s")
        print(f"     Median: {startup_stats['median']:.3f}s")
        print(f"     Std Dev: {startup_stats['std']:.3f}s")
        print(f"     Range: {startup_stats['min']:.3f}s - {startup_stats['max']:.3f}s")
        print(f"     Samples: {startup_stats['count']}")
        
        # Memory statistics
        memory_stats = calculate_statistics(data[image_type]['memory'])
        print(f"  💾 Memory Usage (MB):")
        print(f"     Mean: {memory_stats['mean']:.1f}MB")
        print(f"     Median: {memory_stats['median']:.1f}MB")
        print(f"     Std Dev: {memory_stats['std']:.1f}MB")
        print(f"     Range: {memory_stats['min']:.1f}MB - {memory_stats['max']:.1f}MB")
        print(f"     Samples: {memory_stats['count']}")

def calculate_improvements(data: Dict):
    """Calculate performance improvements of Native vs JVM"""
    jvm_startup = calculate_statistics(data['jvm']['startup'])
    native_startup = calculate_statistics(data['native']['startup'])
    jvm_memory = calculate_statistics(data['jvm']['memory'])
    native_memory = calculate_statistics(data['native']['memory'])
    
    print("\n⚡ Performance Improvements (Native vs JVM)")
    print("=" * 50)
    
    if jvm_startup['mean'] > 0 and native_startup['mean'] > 0:
        startup_improvement = ((jvm_startup['mean'] - native_startup['mean']) / jvm_startup['mean']) * 100
        print(f"🚀 Startup Time: {startup_improvement:.1f}% faster")
        
        if startup_improvement > 0:
            speedup = jvm_startup['mean'] / native_startup['mean']
            print(f"   Native is {speedup:.1f}x faster at startup")
        
    if jvm_memory['mean'] > 0 and native_memory['mean'] > 0:
        memory_improvement = ((jvm_memory['mean'] - native_memory['mean']) / jvm_memory['mean']) * 100
        print(f"💾 Memory Usage: {memory_improvement:.1f}% less memory")
        
        if memory_improvement > 0:
            memory_ratio = jvm_memory['mean'] / native_memory['mean']
            print(f"   Native uses {memory_ratio:.1f}x less memory")

def create_visualizations(data: Dict, output_dir: str):
    """Create visualization charts"""
    if not HAS_MATPLOTLIB:
        print("📊 Skipping visualizations (matplotlib not available)")
        return
    
    print("📊 Creating visualizations...")
    
    # Set up the plot style
    plt.style.use('default')
    fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 10))
    fig.suptitle('Kafka JVM vs Native Image Benchmark Results', fontsize=16, fontweight='bold')
    
    # Colors
    jvm_color = '#ff6b6b'
    native_color = '#4ecdc4'
    
    # 1. Startup Time Comparison (Box Plot)
    startup_data = [data['jvm']['startup'], data['native']['startup']]
    startup_labels = ['JVM', 'Native']
    box1 = ax1.boxplot(startup_data, labels=startup_labels, patch_artist=True)
    box1['boxes'][0].set_facecolor(jvm_color)
    box1['boxes'][1].set_facecolor(native_color)
    ax1.set_title('Startup Time Distribution')
    ax1.set_ylabel('Time (seconds)')
    ax1.grid(True, alpha=0.3)
    
    # 2. Memory Usage Comparison (Box Plot)
    memory_data = [data['jvm']['memory'], data['native']['memory']]
    memory_labels = ['JVM', 'Native']
    box2 = ax2.boxplot(memory_data, labels=memory_labels, patch_artist=True)
    box2['boxes'][0].set_facecolor(jvm_color)
    box2['boxes'][1].set_facecolor(native_color)
    ax2.set_title('Memory Usage Distribution')
    ax2.set_ylabel('Memory (MB)')
    ax2.grid(True, alpha=0.3)
    
    # 3. Startup Time Scatter Plot
    jvm_startup_x = ['JVM'] * len(data['jvm']['startup'])
    native_startup_x = ['Native'] * len(data['native']['startup'])
    
    ax3.scatter(jvm_startup_x, data['jvm']['startup'], color=jvm_color, alpha=0.6, s=50, label='JVM')
    ax3.scatter(native_startup_x, data['native']['startup'], color=native_color, alpha=0.6, s=50, label='Native')
    
    # Add mean lines
    if data['jvm']['startup']:
        ax3.axhline(y=mean(data['jvm']['startup']), color=jvm_color, linestyle='--', alpha=0.8)
    if data['native']['startup']:
        ax3.axhline(y=mean(data['native']['startup']), color=native_color, linestyle='--', alpha=0.8)
    
    ax3.set_title('Startup Time Individual Measurements')
    ax3.set_ylabel('Time (seconds)')
    ax3.legend()
    ax3.grid(True, alpha=0.3)
    
    # 4. Memory Usage Scatter Plot
    jvm_memory_x = ['JVM'] * len(data['jvm']['memory'])
    native_memory_x = ['Native'] * len(data['native']['memory'])
    
    ax4.scatter(jvm_memory_x, data['jvm']['memory'], color=jvm_color, alpha=0.6, s=50, label='JVM')
    ax4.scatter(native_memory_x, data['native']['memory'], color=native_color, alpha=0.6, s=50, label='Native')
    
    # Add mean lines
    if data['jvm']['memory']:
        ax4.axhline(y=mean(data['jvm']['memory']), color=jvm_color, linestyle='--', alpha=0.8)
    if data['native']['memory']:
        ax4.axhline(y=mean(data['native']['memory']), color=native_color, linestyle='--', alpha=0.8)
    
    ax4.set_title('Memory Usage Individual Measurements')
    ax4.set_ylabel('Memory (MB)')
    ax4.legend()
    ax4.grid(True, alpha=0.3)
    
    plt.tight_layout()
    
    # Save the plot
    output_file = os.path.join(output_dir, f"benchmark_comparison_{datetime.now().strftime('%Y%m%d_%H%M%S')}.png")
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"📈 Chart saved: {output_file}")
    
    # Show if not in headless mode
    try:
        plt.show()
    except:
        print("Display not available - chart saved to file only")

def export_data(data: Dict, output_dir: str):
    """Export data to JSON for further analysis"""
    # Calculate statistics
    export_data = {
        'timestamp': datetime.now().isoformat(),
        'summary': {},
        'raw_data': data
    }
    
    for image_type in ['jvm', 'native']:
        export_data['summary'][image_type] = {
            'startup': calculate_statistics(data[image_type]['startup']),
            'memory': calculate_statistics(data[image_type]['memory'])
        }
    
    # Calculate improvements
    if (data['jvm']['startup'] and data['native']['startup'] and 
        data['jvm']['memory'] and data['native']['memory']):
        
        jvm_startup_mean = mean(data['jvm']['startup'])
        native_startup_mean = mean(data['native']['startup'])
        jvm_memory_mean = mean(data['jvm']['memory'])
        native_memory_mean = mean(data['native']['memory'])
        
        export_data['improvements'] = {
            'startup_percentage': ((jvm_startup_mean - native_startup_mean) / jvm_startup_mean) * 100,
            'memory_percentage': ((jvm_memory_mean - native_memory_mean) / jvm_memory_mean) * 100,
            'startup_speedup': jvm_startup_mean / native_startup_mean,
            'memory_ratio': jvm_memory_mean / native_memory_mean
        }
    
    # Export to JSON
    output_file = os.path.join(output_dir, f"benchmark_data_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
    with open(output_file, 'w') as f:
        json.dump(export_data, f, indent=2)
    
    print(f"📁 Data exported: {output_file}")

def main():
    parser = argparse.ArgumentParser(description='Analyze Kafka benchmark results')
    parser.add_argument('--results-dir', '-r', default='benchmark-results', 
                        help='Directory containing benchmark results')
    parser.add_argument('--output-dir', '-o', default='analysis-output',
                        help='Output directory for analysis results')
    parser.add_argument('--no-charts', action='store_true',
                        help='Skip chart generation')
    parser.add_argument('--export-json', action='store_true',
                        help='Export data to JSON')
    
    args = parser.parse_args()
    
    # Check if results directory exists
    if not os.path.exists(args.results_dir):
        print(f"❌ Results directory not found: {args.results_dir}")
        print("Run the benchmark script first to generate results.")
        sys.exit(1)
    
    # Create output directory
    os.makedirs(args.output_dir, exist_ok=True)
    
    print("🔬 Kafka Benchmark Results Analyzer")
    print("=" * 40)
    print(f"📂 Results directory: {args.results_dir}")
    print(f"📂 Output directory: {args.output_dir}")
    print()
    
    # Load data
    print("📖 Loading benchmark data...")
    data = load_benchmark_data(args.results_dir)
    
    # Check if we have data
    total_samples = (len(data['jvm']['startup']) + len(data['jvm']['memory']) + 
                    len(data['native']['startup']) + len(data['native']['memory']))
    
    if total_samples == 0:
        print("❌ No benchmark data found in results directory")
        print("Make sure you've run the benchmark script and it completed successfully.")
        sys.exit(1)
    
    print(f"✅ Loaded data from {total_samples} measurements")
    
    # Print statistics
    print_statistics(data)
    
    # Calculate improvements
    calculate_improvements(data)
    
    # Create visualizations
    if not args.no_charts:
        create_visualizations(data, args.output_dir)
    
    # Export data
    if args.export_json:
        export_data(data, args.output_dir)
    
    print("\n✅ Analysis complete!")

if __name__ == '__main__':
    main() 
