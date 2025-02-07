package com.schedule.energysaving;

import java.util.ArrayList;
import java.util.Collections;

import com.datacenter.DataCenter;
import com.datacenter.DataCenterFactory;
import com.datacenter.LoadBalanceFactory;
import com.generaterequest.CreateLLNLRequests;
import com.generaterequest.CreateVM;
import com.generaterequest.PMBootor;
import com.resource.PhysicalMachine;
import com.resource.VirtualMachine;
import com.schedule.loadbalance.*;
import javax.swing.*;

import com.comparedindex.CalAverageUtility;
import com.comparedindex.CalCapacityMakespan;
//import com.datacenter.DataCenter;
//import com.datacenter.DataCenterFactory;
//import com.datacenter.LoadBalanceFactory;
//import com.generaterequest.CreateLLNLRequests;
import com.generaterequest.CreateVMByPorcessTime;
//import com.generaterequest.PMBootor;
//import com.resource.PhysicalMachine;
//import com.resource.VirtualMachine;
import com.schedule.loadbalance.OfflineAlgorithm;
//import java.util.ArrayList;
//import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

/**
 * maybe PMG
 * @author Yueming Chen
 *
 */
public class EnergySavingMIG extends OfflineAlgorithm {

    int dataCenterIndex; // Selected data center ID
    int rackIndex; // Selected rack ID
    int index; 	//Allocated PM ID
    int currentTime = 0;
    int vmId = 0;  	//vmId is the the id in sorted in vmQueue
    int pmTotalNum;
    int increase = 1;
    int decrease = -1;
    int rejectedVM;
    double threshold = 0.9;
    Random random = new Random();
    VirtualMachine vm;
    ArrayList<VirtualMachine> vmQueue = new ArrayList<VirtualMachine>();
    ArrayList<DataCenter> arr_dc = new ArrayList<DataCenter>();
    ArrayList<VirtualMachine> deleteQueue = new ArrayList<VirtualMachine>();
    int triedAllocationTimes = 0;
    CalAverageUtility cal;
    //ArrayList<DataCenter> cal = new ArrayList<DataCenter>();

    int pmQueueOneSize;
    int pmQueueTwoSize;
    int pmQueueThreeSize;
    
    public EnergySavingMIG() {
        //	System.out.println(getDescription());
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return description + "-PMG Algorithm---";
    }

    @Override
    public void createVM(DataCenterFactory dcf) {
        //	lbf.createVM(new CreateVMByPorcessTime(new CreateVM()));
        //dcf.createVM(new CreateVMByPorcessTime(new CreateLLNLRequests()));
        //dcf.createVM(new CreateVMByPorcessTime(new CreateVM()));
    }

    /**
     * Generate the random index and try to allocate VM to the PM with generated
     * index.
     */
    @Override
    public void allocate(ArrayList<VirtualMachine> p_vmQueue, ArrayList<DataCenter> p_arr_dc) {
        // TODO Auto-generated method stub
        DataCenterFactory.print.println(getDescription());

        Collections.sort(p_vmQueue,new SortByProcessingTime());

        this.vmQueue = p_vmQueue;
        this.arr_dc = p_arr_dc;

        pmQueueOneSize = arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueOne().size();
        pmQueueTwoSize = arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueTwo().size();
        pmQueueThreeSize = arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueThree().size();

        pmTotalNum = pmQueueOneSize + pmQueueTwoSize + pmQueueThreeSize;
        int allocatedDataCenterID;
        int allocatedRackID;

        for(int i = 0 ; i < vmQueue.size() ; i++){
            vmQueue.get(i).setVmNo(i);
        }


        DataCenterFactory.print.println("===currentTime:" + currentTime + "===");
        while (!vmQueue.isEmpty()) {
            if (currentTime >= vmQueue.get(vmId).getStartTime()) {
                vm = vmQueue.get(vmId);
            } else {
                vmId++;
                triedAllocationTimes = 0;
                checkVmIdAvailable();
                continue;
            }

            //随机寻找pm分配
            //对数据中心按CM排序
            Collections.sort(arr_dc, new SortByDataCenterCapacityMakespan());
            dataCenterIndex = 0;
            allocatedDataCenterID = arr_dc.get(dataCenterIndex).getD_id();

            //机架级别的排序
            Collections.sort(arr_dc.get(dataCenterIndex).getArr_lbf(), new SortByRackCapacityMakespan());
            rackIndex = 0;
            allocatedRackID = arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getLbf_ID();

            //这里将所有的请求都用pm1模拟；后期需要拓展
            if(vm.getVmType() > 0 && vm.getVmType() < 4){
                allocateVm(allocatedDataCenterID,allocatedRackID,vm,arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueOne().get(index));
            }
            else if (vm.getVmType() >= 4 && vm.getVmType() < 7){
                allocateVm(allocatedDataCenterID,allocatedRackID,vm,arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueTwo().get(index));
            }
            else{
                allocateVm(allocatedDataCenterID,allocatedRackID,vm,arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueThree().get(index));
            }
        }

        //迁移
        DataCenterFactory.print.println("Start Re-allocation......");
        //Max total capacitymakespan multiply the threshold factor
        //threshold *= getThreshold();
        threshold = threshold * (cm / pmTotalNum);
        System.out.println("threshold的值为=" + threshold);

        if(vm.getVmType() > 0 && vm.getVmType() < 4){
            //Collections.sort(arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueOne(), new SortByCapacityMakespan());
            reallocateVm(arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueOne());
        }
        else if (vm.getVmType() >= 4 && vm.getVmType() < 7){
            //Collections.sort(arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueTwo(), new SortByCapacityMakespan());
            reallocateVm(arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueTwo());
        }
        else{
            //Collections.sort(arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueThree(), new SortByCapacityMakespan());
            reallocateVm(arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueThree());
        }
        //reallocateVm(arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueOne());
        //reallocateVm(arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueTwo());
        //reallocateVm(arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueThree());
        DataCenterFactory.print.println(LoadBalanceFactory.FINISHEDINFO);
    }

    /**
     * Key scheduling procedure for algorithm. Main procedures are as below: 1.
     * Check whether resource of a PM is available. 2. If resource available,
     * output success information. Put the VM to deleteQueue, and remove that VM
     * from vmQueue. 3. Update available resource of PM.
     *
     * @param vm2
     * @param pm2
     */
    double cm = 0;
    private void allocateVm(int dataCenterNo, int rackNo, VirtualMachine vm2, PhysicalMachine pm2) {
        // TODO Auto-generated method stub
        if (checkResourceAvailble(vm2, pm2)) {
            DataCenterFactory.print.println("Allocate:VM" + vm2.getVmNo() + " " + "to DataCenter" + dataCenterNo + " Rack" + rackNo + " PM" + pm2.getNo());
            deleteQueue.add(vm2);
            vmQueue.remove(vm2);
            pm2.vms.add(vm2);
            cm += vm2.getCpuTotal()*(vm2.getEndTime()-vm2.getStartTime());
            vm2.setPmNo(pm2.getNo());
            vm2.setRackNo(rackNo);
            vm2.setDataCenterNo(dataCenterNo);

            updateResource(vm2, pm2, decrease);
            sortPM(vm2);

            vmId = 0;
            triedAllocationTimes = 0;
            checkVmIdAvailable();
            index = 0;
        } else {
            if (triedAllocationTimes == pmTotalNum) {
                System.out
                        .println("VM number is too large, PM number is not enough");
                JOptionPane.showMessageDialog(null,
                        "VM number is too large, PM number is not enough",
                        "Error", JOptionPane.OK_OPTION);
                throw new IllegalArgumentException("PM too less");
            } else {
                triedAllocationTimes++;
                DataCenterFactory.print.println(DataCenterFactory.FAILEDINFO);
                index++; // Try another PM
            }
        }
    }

    /**
     * Check whether the vmId has surpassed bound, if yes, reset vmId as 0.
     */
    private void checkVmIdAvailable() {
        if (vmId >= vmQueue.size()) {
            currentTime++;
            vmId = 0;
            triedAllocationTimes = 0;
            DataCenterFactory.print.println("===currentTime:" + currentTime
                    + "===");
            processDeleteQueue(currentTime, deleteQueue);
        }
    }

    /**
     * Check whether the left resource are available
     *
     * @param vm3
     * @param pm3
     * @return
     */
    private boolean checkResourceAvailble(VirtualMachine vm3,
                                          PhysicalMachine pm3) {
        boolean allocateSuccess = true;
        boolean oneSlotAllocation;
        for (int t = vm3.getStartTime(); t < vm3.getEndTime(); t++) {
            oneSlotAllocation = (pm3.resource.get(t).getCpuUtility() > vm3
                    .getCpuTotal())
                    && (pm3.resource.get(t).getMemUtility() > vm3.getMemTotal())
                    && (pm3.resource.get(t).getStoUtility() > vm3
                    .getStorageTotal());
            allocateSuccess = allocateSuccess && oneSlotAllocation;

            if (false == allocateSuccess) {
                // If allocated failed, return exactly.
                return allocateSuccess;
            }
        }
        return allocateSuccess;
    }

    /**
     * Update the available resource. When parameter 3 equals to increase,
     * available resource would increased, else resource would be decreased.
     *
     * @param vm4
     * @param pm4
     * @param incOrDec
     */
    private void updateResource(VirtualMachine vm4, PhysicalMachine pm4,
                                int incOrDec) {
        if (incOrDec == decrease) {
            for (int t = vm4.getStartTime(); t < vm4.getEndTime(); t++) {
                pm4.resource.get(t)
                        .setCpuUtility(
                                pm4.resource.get(t).getCpuUtility() - vm4.getCpuTotal());
                pm4.resource.get(t)
                        .setMemUtility(
                                pm4.resource.get(t).getMemUtility() - vm4.getMemTotal());
                pm4.resource.get(t).setStoUtility(
                        pm4.resource.get(t).getStoUtility() - vm4.getStorageTotal());
            }
            DataCenterFactory.print.println("Resource is updated(dec)");
        }
        if (incOrDec == increase) {
            for (int t = vm4.getStartTime(); t < vm4.getEndTime(); t++) {
                pm4.resource.get(t)
                        .setCpuUtility(
                                pm4.resource.get(t).getCpuUtility() + vm4.getCpuTotal());
                pm4.resource.get(t)
                        .setMemUtility(
                                pm4.resource.get(t).getMemUtility() + vm4.getMemTotal());
                pm4.resource.get(t).setStoUtility(
                        pm4.resource.get(t).getStoUtility() + vm4.getStorageTotal());
            }
            DataCenterFactory.print.println("Remove:VM" + vm4.getVmNo()
                    + " from DataCenter" + vm4.getDataCenterNo() + " Rack"
                    + vm4.getRackNo() + " PM" + pm4.getNo());
            DataCenterFactory.print.println("Resource is updated(inc)");
        }
    }

    /**
     * After the VM has been added to deleteQueue, if end time comes, that VM
     * should be removed from deleteQueue. Available resource should also be
     * updated.
     *
     * @param p_currentTime
     * @param p_deleteQueue
     */
    private void processDeleteQueue(int p_currentTime,
                                    ArrayList<VirtualMachine> p_deleteQueue) {
        // TODO Auto-generated method stub
        VirtualMachine vm5;
        int pmNo;
        int dataCenterNo;
        int rackNo;

        for (int i = 0; i < p_deleteQueue.size(); i++) {
            vm5 = p_deleteQueue.get(i);
            dataCenterNo = vm5.getDataCenterNo();
            rackNo = vm5.getRackNo();
            pmNo = vm5.getPmNo();

            if (p_currentTime >= vm5.getEndTime()) {
                if (pmNo >= 0 && pmNo < pmQueueOneSize) {
                    updateResource(vm5, arr_dc.get(dataCenterNo).getArr_lbf()
                            .get(rackNo).getPmQueueOne().get(pmNo), increase);
                } else if (pmNo >= pmQueueOneSize
                        && pmNo < pmQueueOneSize + pmQueueTwoSize) {
                    updateResource(
                            vm5,
                            arr_dc.get(dataCenterNo).getArr_lbf().get(rackNo)
                                    .getPmQueueTwo().get(pmNo - pmQueueOneSize),
                            increase);
                } else {
                    updateResource(
                            vm5,
                            arr_dc.get(dataCenterNo)
                                    .getArr_lbf()
                                    .get(rackNo)
                                    .getPmQueueThree()
                                    .get(pmNo - pmQueueOneSize
                                            - pmQueueTwoSize), increase);
                }
                p_deleteQueue.remove(vm5);
            }
        }
    }

    private void sortPM(VirtualMachine vm1) {
        if(vm1.getVmType() > 0 && vm1.getVmType() < 4) {
            Collections.sort(arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueOne(), new SortByCurrentUtility(currentTime));
        }
        else if(vm1.getVmType() >= 4 && vm1.getVmType() < 7){
            Collections.sort(arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueTwo(), new SortByCurrentUtility(currentTime));
        }
        else{
            Collections.sort(arr_dc.get(dataCenterIndex).getArr_lbf().get(rackIndex).getPmQueueThree(), new SortByCurrentUtility(currentTime));
        }
    }
    
    /**
     * Get the maximum capacity-makespan
     * @return 
     */
    public float getThreshold(){
        cal = new CalAverageUtility(arr_dc);
        return new CalCapacityMakespan(cal).getIndexValue();
        //cal = new CalAverageUtility(arr_dc);
        //System.out.println("cal的值为："+cal);
        //return new CalCapacityMakespan(arr_dc).getIndexValue();
    }
    /**
     * Migrate the vms from the PM with highest CM value to PM with lowest CM value
     */
    private void reallocateVm(ArrayList<PhysicalMachine> pmQueue){
        int size = pmQueue.size();
        PhysicalMachine pm1, pm2;
        VirtualMachine vm1;
        //Collections.sort(pmQueue, new SortByCapacityMakespan());
        for (int i = 0 ; i < pmQueue.size(); i++) {
            Collections.sort(pmQueue, new SortByCapacityMakespan());
            if (pmQueue.get(0).getTotalCapacityMakespan() > threshold) {
                pm1 = pmQueue.get(0);
                pm2 = pmQueue.get(size - 1);
                vm1 = pm1.vms.get(0);
                if (checkResourceAvailble(vm1, pm2)) {
                    pm1.vms.remove(vm1);
                    updateResource(vm1, pm1, increase);
                    pm2.vms.add(vm1);
                    vm1.setPmNo(pm2.getNo());
                    updateResource(vm1, pm2, decrease);
                } else {
                    DataCenterFactory.print.println("Migration Failed!");
                }
            } else {
                DataCenterFactory.print.println("Migartion Finished......");
                break;
            }
        }

//        pm1 = pmQueue.get(0);
//        pm2 = pmQueue.get(size - 1);
//        if(pm1.getTotalCapacityMakespan() - pm2.getTotalCapacityMakespan() > threshold){
//            vm1 = pm1.vms.get(0);
//            if(checkResourceAvailble(vm1, pm2)){
//                pm1.vms.remove(vm1);
//                updateResource(vm1, pm1, increase);
//                pm2.vms.add(vm1);
//                vm1.setPmNo(pm2.getNo());
//                updateResource(vm1, pm2, decrease);
//                reallocateVm(pmQueue);
//            }else{
//                DataCenterFactory.print.println("Migration Failed!");
//            }
//        }else{
//            DataCenterFactory.print.println("Migartion Finished......");
//        }
    }
}


class SortByCapacityMakespan implements Comparator<PhysicalMachine> {
    public int compare(PhysicalMachine o1, PhysicalMachine o2){
        PhysicalMachine pm1 = o1;
        PhysicalMachine pm2 = o2;
        if(pm1.getTotalCapacityMakespan() > pm2.getTotalCapacityMakespan())
               return 1; 
        return 0;
    }
}


//public class EnergySavingMIG extends OfflineAlgorithm {
//
//    int index; 	//Allocated PM ID
//    int currentTime = 0;
//    int vmId = 0;  	//vmId is the the id in sorted in vmQueue
//    int pmTotalNum;
//    int increase = 1;
//    int decrease = -1;
//    int rejectedVM;
//    float threshold = 0.2f;
//    Random random = new Random();
//    VirtualMachine vm;
//    ArrayList<VirtualMachine> vmQueue = new ArrayList<VirtualMachine>();
//    ArrayList<PhysicalMachine> pmQueueOne = new ArrayList<PhysicalMachine>();
//    ArrayList<PhysicalMachine> pmQueueTwo = new ArrayList<PhysicalMachine>();
//    ArrayList<PhysicalMachine> pmQueueThree = new ArrayList<PhysicalMachine>();
//    ArrayList<VirtualMachine> deleteQueue = new ArrayList<VirtualMachine>();
//    CalAverageUtility cal;
//
//    public EnergySavingMIG() {
//        //	System.out.println(getDescription());
//    }
//
//    @Override
//    public String getDescription() {
//        // TODO Auto-generated method stub
//        return description + "-MIG Algorithm---";
//    }
//
//    @Override
//    public void createVM(LoadBalanceFactory lbf) {
//        //	lbf.createVM(new CreateVMByPorcessTime(new CreateVM()));
//        lbf.createVM(new CreateVMByPorcessTime(new CreateLLNLRequests()));
//    }
//
//    /**
//     * Generate the random index and try to allocate VM to the PM with generated
//     * index.
//     */
//    @Override
//    public void allocate(ArrayList<VirtualMachine> p_vmQueue,
//                         ArrayList<PhysicalMachine> p_pmQueueOne,
//                         ArrayList<PhysicalMachine> p_pmQueueTwo,
//                         ArrayList<PhysicalMachine> p_pmQueueThree) {
//        // TODO Auto-generated method stub
//        this.vmQueue = p_vmQueue;
//        this.pmQueueOne = p_pmQueueOne;
//        this.pmQueueTwo = p_pmQueueTwo;
//        this.pmQueueThree = p_pmQueueThree;
//
////        System.out.println(pmQueueOne.get(0).resource.get(0).getCpuUtility());
////        System.out.println(pmQueueOne.get(0).resource.get(0).getMemUtility());
////        System.out.println(pmQueueOne.get(0).resource.get(0).getStoUtility());
//
//        pmTotalNum = pmQueueOne.size() + pmQueueTwo.size() + pmQueueThree.size();
//        while (!vmQueue.isEmpty()) {
//            //Pick out the vm with startTime less than currentTime to allocate
//            if (currentTime >= vmQueue.get(vmId).getStartTime()) {
//                vm = vmQueue.get(vmId);
//            } else {
//                vmId++;
//                //Refactored method, see details in definition.
//                checkVmIdAvailable();
//                continue;
//            }
//
//            //Randomly find a PM
//            index = 0;
//            LoadBalanceFactory.print.println("MIG-Index:" + index);
//            //Three queues should be decided which queue would be added.
//            System.out.println(vm.getVmType());
//            if (vm.getVmType() > 0 && vm.getVmType() <= 3) {
//                System.out.println(pmQueueThree);
//                allocateVm(vm, pmQueueThree.get(index));
//            } else if (vm.getVmType() > 3 && vm.getVmType() <= 6) {
//                allocateVm(vm, pmQueueTwo.get(index));
//            } else {
//                allocateVm(vm, pmQueueThree.get(index));
//            }
//        }
//        LoadBalanceFactory.print.println("Start Re-allocation......");
//        //Max total capacitymakespan multiply the threshold factor
//        threshold *= getThreshold();
//        reallocateVm(pmQueueThree);
//        reallocateVm(pmQueueTwo);
//        reallocateVm(pmQueueThree);
//
//        LoadBalanceFactory.print.println(LoadBalanceFactory.FINISHEDINFO);
//    }
//
//    /**
//     * Key scheduling procedure for algorithm. Main procedures are as below: 1.
//     * Check whether resource of a PM is available. 2. If resource available,
//     * output success information. Put the VM to deleteQueue, and remove that VM
//     * from vmQueue. 3. Update available resource of PM.
//     *
//     * @param vm2
//     * @param pm2
//     */
//    private void allocateVm(VirtualMachine vm2, PhysicalMachine pm2) {
//        // TODO Auto-generated method stub
//        if (checkResourceAvailble(vm2, pm2)) {
//            LoadBalanceFactory.print.println("Allocate:VM" + vm2.getVmNo() + " to PM" + pm2.getNo());
//            deleteQueue.add(vm2);
//            vmQueue.remove(vm2);
//            pm2.vms.add(vm2);
//            vm2.setPmNo(pm2.getNo());
//            updateResource(vm2, pm2, decrease);
//            sortPM(vm2);
//
//            vmId++;
//            checkVmIdAvailable();
//        } else {
//            vmQueue.remove(vm2); //Though the request can not be allocated, that VM should be removed.
//            LoadBalanceFactory.print.println("VM is rejected");
//            rejectedVM = pm2.getRejectedNum();
//            pm2.setRejectedNum(rejectedVM + 1);
//
//            vmId++;
//            checkVmIdAvailable();
//        }
//    }
//
//    /**
//     * Check whether the vmId has surpassed bound, if yes, reset vmId as 0.
//     */
//    private void checkVmIdAvailable() {
//        if (vmId >= vmQueue.size()) {
//            currentTime++;
//            vmId = 0;
//            LoadBalanceFactory.print.println("===currentTime:" + currentTime + "===");
//            processDeleteQueue(currentTime, deleteQueue);
//        }
//    }
//
//    /**
//     * Check whether the left resource are available
//     *
//     * @param vm3
//     * @param pm3
//     * @return
//     */
//    private boolean checkResourceAvailble(VirtualMachine vm3, PhysicalMachine pm3) {
//        boolean allocateSuccess = true;
//        boolean oneSlotAllocation;
//        for (int t = vm3.getStartTime(); t < vm3.getEndTime(); t++) {
//            oneSlotAllocation = (pm3.resource.get(t).getCpuUtility() > vm3.getCpuTotal())
//                    && (pm3.resource.get(t).getMemUtility() > vm3.getMemTotal())
//                    && (pm3.resource.get(t).getStoUtility() > vm3.getStorageTotal());
//            allocateSuccess = allocateSuccess && oneSlotAllocation;
//            //	System.out.println(pm3.resource.get(t).getCpuUtility() +" "+ vm3.getCpuTotal());
//            //	System.out.println(pm3.resource.get(t).getMemUtility() +" "+ vm3.getMemTotal());
//            //	System.out.println(pm3.resource.get(t).getStoUtility() +" "+ vm3.getStorageTotal());
//
//            if (false == allocateSuccess) {
//                //If allocated failed, return exactly.
//                return allocateSuccess;
//            }
//        }
//        return allocateSuccess;
//    }
//
//    /**
//     * Update the available resource. When parameter 3 equals to increase,
//     * available resource would increased, else resource would be decreased.
//     *
//     * @param vm4s
//     * @param pm4
//     * @param incOrDec
//     */
//    private void updateResource(VirtualMachine vm4, PhysicalMachine pm4, int incOrDec) {
//        if (incOrDec == decrease) {
//            for (int t = vm4.getStartTime(); t < vm4.getEndTime(); t++) {
//                pm4.resource.get(t).setCpuUtility(
//                        pm4.resource.get(t).getCpuUtility() - vm4.getCpuTotal());
//                pm4.resource.get(t).setMemUtility(
//                        pm4.resource.get(t).getMemUtility() - vm4.getMemTotal());
//                pm4.resource.get(t).setStoUtility(
//                        pm4.resource.get(t).getStoUtility() - vm4.getStorageTotal());
//            }
//            LoadBalanceFactory.print.println("Resource is updated(dec)");
//        }
//        if (incOrDec == increase) {
//            for (int t = vm4.getStartTime(); t < vm4.getEndTime(); t++) {
//                pm4.resource.get(t).setCpuUtility(
//                        pm4.resource.get(t).getCpuUtility() + vm4.getCpuTotal());
//                pm4.resource.get(t).setMemUtility(
//                        pm4.resource.get(t).getMemUtility() + vm4.getMemTotal());
//                pm4.resource.get(t).setStoUtility(
//                        pm4.resource.get(t).getStoUtility() + vm4.getStorageTotal());
//            }
//            LoadBalanceFactory.print.println("Remove:VM" + vm4.getVmNo() + " from PM" + pm4.getNo());
//            LoadBalanceFactory.print.println("Resource is updated(inc)");
//
//        }
//    }
//
//    /**
//     * After the VM has been added to deleteQueue, if end time comes, that VM
//     * should be removed from deleteQueue. Available resource should also be
//     * updated.
//     *
//     * @param p_currentTime
//     * @param p_deleteQueue
//     */
//    private void processDeleteQueue(int p_currentTime, ArrayList<VirtualMachine> p_deleteQueue) {
//        // TODO Auto-generated method stub
//        VirtualMachine vm5;
//        PMBootor pmb1 = new PMBootor();
//        ArrayList<Integer> pmNum = pmb1.bootPM();
//        int pmNo;
//
//        for (int i = 0; i < p_deleteQueue.size(); i++) {
//            vm5 = p_deleteQueue.get(i);
//            pmNo = vm5.getPmNo();
//            if (p_currentTime >= vm5.getEndTime()) {
//                if (pmNo >= 0 && pmNo < pmNum.get(0)) {
//                    for (int j = 0; j < pmNum.get(0); j++) {
//                        if (pmNo == pmQueueThree.get(j).getNo()) {
//                            updateResource(vm5, pmQueueThree.get(j), increase);
//                            break;
//                        }
//                    }
//                } else if (pmNo >= pmNum.get(0) && pmNo < pmNum.get(0) + pmNum.get(1)) {
//                    for (int j = 0; j < pmNum.get(1); j++) {
//                        if (pmNo == pmQueueTwo.get(j).getNo()) {
//                            updateResource(vm5, pmQueueTwo.get(j), increase);
//                            break;
//                        }
//                    }
//                } else {
//                    for (int j = 0; j < pmNum.get(2); j++) {
//                        if (pmNo == pmQueueThree.get(j).getNo()) {
//                            updateResource(vm5, pmQueueThree.get(j), increase);
//                            break;
//                        }
//                    }
//                }
//                p_deleteQueue.remove(vm5);
//            }
//        }
//    }
//
//    private void sortPM(VirtualMachine vm1) {
//        if (vm1.getVmType() > 0 && vm1.getVmType() <= 3) {
//            Collections.sort(pmQueueThree, new SortByCurrentUtility(currentTime));
//        } else if (vm1.getVmType() > 3 && vm1.getVmType() <= 6) {
//            Collections.sort(pmQueueTwo, new SortByCurrentUtility(currentTime));
//        } else {
//            Collections.sort(pmQueueThree, new SortByCurrentUtility(currentTime));
//        }
//    }
//
//    /**
//     * Get the maximum capacity-makespan
//     * @return
//     */
//    public float getThreshold(){
//        cal = new CalAverageUtility(new ArrayList<DataCenter>());
//        return new CalCapacityMakespan(cal).getIndexValue();
//    }
//    /**
//     * Migrate the vms from the PM with highest CM value to PM with lowest CM
//     * value
//     * @param pmQueue
//     */
//    private void reallocateVm(ArrayList<PhysicalMachine> pmQueue){
//        int size = pmQueue.size();
//        PhysicalMachine pm1, pm2;
//        VirtualMachine vm1;
//        Collections.sort(pmQueue, new SortByCapacityMakespan());
//        pm1 = pmQueue.get(0);
//        pm2 = pmQueue.get(size - 1);
//        if(pm1.getTotalCapacityMakespan()
//                - pm2.getTotalCapacityMakespan() > threshold){
//            vm1 = pm1.vms.get(0);
//            if(checkResourceAvailble(vm1, pm2)){
//                pm1.vms.remove(vm1);
//                updateResource(vm1, pm1, increase);
//                pm2.vms.add(vm1);
//                vm1.setPmNo(pm2.getNo());
//                updateResource(vm1, pm2, decrease);
//                reallocateVm(pmQueue);
//            }else{
//                LoadBalanceFactory.print.println("Migration Failed!");
//            }
//        }else{
//            LoadBalanceFactory.print.println("Migartion Finished......");
//        }
//    }
//}
//
//
//class SortByCapacityMakespan implements Comparator<PhysicalMachine> {
//    SortByCapacityMakespan() {
//    }
//
//    /**
//     * Decreasing order
//     * @param o1
//     * @param o2
//     * @return
//     */
//    public int compare(PhysicalMachine o1, PhysicalMachine o2){
//        PhysicalMachine pm1 = o1;
//        PhysicalMachine pm2 = o2;
//        if(pm1.getTotalCapacityMakespan() < pm2.getTotalCapacityMakespan())
//            return 1;
//        return 0;
//    }
//}