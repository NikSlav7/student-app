package com.example.ResourceServer.pdf;

import com.example.ResourceServer.dao.AbsenceDAO;
import com.example.ResourceServer.dao.PeriodsDao;
import com.example.ResourceServer.dao.YearsDao;
import com.example.ResourceServer.dictionary.Dictionary;
import com.example.ResourceServer.dictionary.Language;
import com.example.ResourceServer.dictionary.LanguageDetector;
import com.example.ResourceServer.domains.*;
import com.example.ResourceServer.dto.AbsenceStatsDTO;
import com.example.ResourceServer.exceptions.WrongFileException;
import com.example.ResourceServer.repositories.ProfilesRepository;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.util.*;
import java.util.stream.Collectors;


@Component
public class MarksManager {


    private final YearsDao yearsDao;

    private final PeriodsDao periodsDao;

    private final ProfilesRepository profilesRepository;

    private final AbsenceDAO absenceStatsDAO;


    public MarksManager(YearsDao yearsDao, PeriodsDao periodsDao, ProfilesRepository profilesRepository, AbsenceDAO absenceStatsDAO) {
        this.yearsDao = yearsDao;
        this.periodsDao = periodsDao;
        this.profilesRepository = profilesRepository;
        this.absenceStatsDAO = absenceStatsDAO;
    }


    public static String extractPeriod(String pdfString, Language language){
        String lineSep = System.getProperty("line.separator");
        String[] strings = pdfString.split(lineSep);
        String label = Dictionary.translate("Periood", language) + ":";
        for (String s : strings){
            if (s.contains(label)) return s.substring(s.indexOf(label) + label.length()).trim().split("\\s+")[0];
        }
        return null;
    }

    public static String extractMarks(PDDocument document) throws WrongFileException {
        StringBuilder stringBuilder = new StringBuilder();
        ObjectExtractor objectExtractor = new ObjectExtractor(document);
        SpreadsheetExtractionAlgorithm spreadsheetExtractionAlgorithm = new SpreadsheetExtractionAlgorithm();
        PageIterator pageIterator = objectExtractor.extract();
        String lineSeparator = System.getProperty("line.separator");
        while (pageIterator.hasNext()){
            Page page = pageIterator.next();
            List<Table> list = spreadsheetExtractionAlgorithm.extract(page);
            if (list.size() == 0) continue;
            Table table = list.get(0);
            for (int i = 1; i < table.getRowCount(); i++){
                stringBuilder.append(table.getCell(i, 0).getText() + "," + table.getCell(i, 3).getText().replace(lineSeparator, "") + lineSeparator);
            }
        }
        return stringBuilder.toString();
    }

    @Transactional
    public List<Subject> createAndSaveMarks(String marksString, AbsenceStats absenceStats, Profile profile, String yearName, String periodName, MarksMode marksMode){
        String lineSeparator = System.getProperty("line.separator");

        Year year = yearsDao.getByNameOrCreateYear(yearName, profile);
        Period period = periodsDao.getByNameOrCreatePeriod(periodName, year);
        String[] subjects = marksString.split(lineSeparator + "|\n");
        List<Subject> markList = new ArrayList<>();
        Upload upload = new Upload();
        upload.setUploadId(UUID.randomUUID().toString());
        upload.setUploadDate(new Date());
        upload.setMarkList(new ArrayList<>());
        for (String subject : subjects) {
            if (subject.isBlank()) continue;
            List<Double> marksList = MarksParser.parseMarks(subject, marksMode);
            OptionalDouble average = MarksParser.average(marksList);
            if (average.isEmpty()) continue;

            Subject sbj = new Subject();
            sbj.setSubjectName(MarksParser.subjectName(subject));
            sbj.setSubjectId(UUID.randomUUID().toString());
            Mark mark = new Mark(UUID.randomUUID().toString(), marksList, average.getAsDouble(), sbj, upload);
            upload.setMarkList(List.of(mark));
            upload.setProfile(profile);
            sbj.setMarkList(List.of(mark));
            sbj.setPeriod(period);
            markList.add(sbj);
        }
        profile.getUploadList().add(upload);



        for (Subject subject : markList){
            List<Subject> temp = new ArrayList<>(period.getSubjectList()).stream().filter(sbj -> sbj.getSubjectName().equals(subject.getSubjectName())).collect(Collectors.toList());
            if (temp.isEmpty()){
                period.getSubjectList().add(subject);
                subject.setPeriod(period);
            }
            else {
                Subject saved = temp.get(0);
                subject.getMarkList().get(0).setSubject(saved);
                saved.getMarkList().add(subject.getMarkList().get(0));
            }
        }
        period.setAbsenceStats(absenceStats);
        year.getPeriodList().add(period);
        year.setProfile(profile);
        period.setYear(year);
        profile.getYearList().add(year);
        absenceStats.setPeriod(period);
        profilesRepository.save(profile);

        return markList;
    }



}
