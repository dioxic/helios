# Requirements

1. I want to be able to run multiple workloads
2. I want to run a workload x times per second
3. I want to run multiple workloads but weighted (0.1 workloadA, 0.9 workloadB)
4. I want to run one workload from a list x times per second
5. I want to name workload for logging/output visibility
6. Sometimes I want to generate stuff every doc run, sometimes I want it fixed
e.g. $choose: { $array: { of: $oid, n: 1000 }}


## Notes

* Should be either workloads /second or weighted but not both at the same time?
* total workloads/s + weighting could be converted to just workload/s on each workload
* workload/s needs to be done on the produce-side

* The total rate limit should apply to all workloads without a specified rate