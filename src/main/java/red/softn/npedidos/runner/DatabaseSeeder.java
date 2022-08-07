package red.softn.npedidos.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.datafaker.DateAndTime;
import net.datafaker.Faker;
import net.datafaker.Food;
import net.datafaker.Options;
import net.datafaker.service.RandomService;
import org.springframework.stereotype.Component;
import red.softn.npedidos.entity.*;
import red.softn.npedidos.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
@Log4j2
public class DatabaseSeeder {
    
    private final Faker faker;
    
    private final ExcludeDayRepository excludeDayRepository;
    
    private final FoodDishRepository foodDishRepository;
    
    private final OrderingRepository orderingRepository;
    
    private final SettingRepository settingRepository;
    
    private final TypeDishRepository typeDishRepository;
    
    private final UserRepository userRepository;
    
    public void db() {
        log.info("Insertando datos de prueba...");
        userFactory(50);
        excludeDayFactory(10);
        typeDishFactory(10);
        foodDishFactory(100);
        orderingFactory(50);
        settingFactory(10);
        relationshipFactory();
        log.info("Proceso finalizado.");
    }
    
    public void fresh() {
        log.info("Eliminando todos los datos...");
        this.excludeDayRepository.deleteAll();
        this.foodDishRepository.deleteAll();
        this.typeDishRepository.deleteAll();
        this.orderingRepository.deleteAll();
        this.settingRepository.deleteAll();
        this.userRepository.deleteAll();
        log.info("Proceso finalizado.");
    }
    
    private void relationshipFactory() {
        Iterable<Ordering> orderings = this.orderingRepository.findAll();
        Iterable<FoodDish> foodDishes = this.foodDishRepository.findAll();
        
        orderings.forEach(value -> {
            List<FoodDish> foodDishList = StreamSupport.stream(foodDishes.spliterator(), false)
                                                       .limit(10)
                                                       .toList();
            
            value.setFoodDishes(foodDishList);
            
            this.orderingRepository.save(value);
        });
        
    }
    
    private void userFactory(int count) {
        log.info("Insertando registros User...");
        run(count, () -> {
            User user = new User();
            user.setEmail(this.faker.internet()
                                    .emailAddress());
            user.setUsername(this.faker.name()
                                       .username());
            user.setPassword(this.faker.internet()
                                       .password(45, 45));
            
            this.userRepository.save(user);
        });
    }
    
    private void typeDishFactory(int count) {
        log.info("Insertando registros TypeDish...");
        run(count, () -> {
            TypeDish typeDish = new TypeDish();
            typeDish.setName(this.faker.funnyName()
                                       .name());
            
            this.typeDishRepository.save(typeDish);
        });
    }
    
    private void settingFactory(int count) {
        log.info("Insertando registros Setting...");
        run(count, () -> {
            Setting setting = new Setting();
            setting.setKeyName(this.faker.letterify("???_???_???"));
            setting.setValue(this.faker.lorem()
                                       .paragraph());
            setting.setDescription(this.faker.lorem()
                                             .sentence(2));
            
            this.settingRepository.save(setting);
        });
    }
    
    private void orderingFactory(int count) {
        log.info("Insertando registros Ordering...");
        DateAndTime date = this.faker.date();
        RandomService random = this.faker.random();
        Iterable<User> all = this.userRepository.findAll();
        List<User> userList = StreamSupport.stream(all.spliterator(), false)
                                           .toList();
        Options options = this.faker.options();
        
        run(count, () -> {
            Ordering ordering = new Ordering();
            LocalDateTime dateNoExclude = getDateNoExclude(date, random);
            
            ordering.setDateOrder(dateNoExclude);
            ordering.setUser(options.nextElement(userList));
            
            this.orderingRepository.save(ordering);
        });
    }
    
    private void foodDishFactory(int count) {
        log.info("Insertando registros FoodDish...");
        var all = this.typeDishRepository.findAll();
        var typeDishList = StreamSupport.stream(all.spliterator(), false)
                                        .toList();
        Options options = this.faker.options();
        Food food = this.faker.food();
        
        run(count, () -> {
            FoodDish foodDish = new FoodDish();
            
            foodDish.setTypeDish(options.nextElement(typeDishList));
            foodDish.setName(food.dish());
            
            this.foodDishRepository.save(foodDish);
        });
    }
    
    private void excludeDayFactory(int count) {
        log.info("Insertando registros ExcludeDay...");
        DateAndTime date = this.faker.date();
        RandomService random = this.faker.random();
        
        run(count, () -> {
            ExcludeDay excludeDay = new ExcludeDay();
            LocalDateTime localDateTime = getDateNoExclude(date, random);
            
            excludeDay.setDateExclude(localDateTime.toLocalDate());
            
            this.excludeDayRepository.save(excludeDay);
        });
    }
    
    private LocalDateTime getDateNoExclude(DateAndTime date, RandomService random) {
        boolean present;
        LocalDateTime localDateTime;
        
        do {
            localDateTime = date.future(random.nextInt(1, 100), TimeUnit.DAYS)
                                .toLocalDateTime();
            present = this.excludeDayRepository.findByDateExclude(localDateTime.toLocalDate())
                                               .isPresent();
        } while (present);
        
        return localDateTime;
    }
    
    private void run(int count, Runnable runnable) {
        for (int i = 0; i < count; i++) {
            runnable.run();
        }
    }
    
}