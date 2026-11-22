package com.budgetbuddy.api.income;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class IncomeService {

    private final IncomeRepository incomeRepository;

    public IncomeService(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    @Transactional(readOnly = true)
    public List<IncomeEntry> getAllIncome(Long userId) {
        return incomeRepository.findAllByUserIdOrderByDateDesc(userId);
    }

    @Transactional(readOnly = true)
    public IncomeEntry getIncome(Long id, Long userId) {
        return findIncome(id, userId);
    }

    public IncomeEntry addIncome(IncomeRequest request, Long userId) {
        validateSchedule(request);
        IncomeEntry income = new IncomeEntry(userId, request.getSource(), request.getAmount(), request.getDate(),
                request.getFrequency(), request.getFrequency().equals("CUSTOM") ? request.getIntervalDays() : null);
        return incomeRepository.save(income);
    }

    public IncomeEntry updateIncome(Long id, IncomeRequest request, Long userId) {
        validateSchedule(request);
        IncomeEntry income = findIncome(id, userId);
        income.updateDetails(request.getSource(), request.getAmount(), request.getDate(), request.getFrequency(),
                request.getFrequency().equals("CUSTOM") ? request.getIntervalDays() : null);
        return incomeRepository.save(income);
    }

    public void deleteIncome(Long id, Long userId) {
        incomeRepository.delete(findIncome(id, userId));
    }

    private IncomeEntry findIncome(Long id, Long userId) {
        return incomeRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Income entry " + id + " was not found"));
    }

    private void validateSchedule(IncomeRequest request) {
        if ("CUSTOM".equals(request.getFrequency()) && request.getIntervalDays() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose how many days apart custom income payments are.");
        }
    }
}
